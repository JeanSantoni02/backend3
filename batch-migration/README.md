# Semana 3 — Migración de procesos batch a Spring Batch

Módulo `batch-migration`. Para la visión general del proyecto completo, ver el [README raíz](../README.md).

Migración de tres procesos legacy del Banco XYZ a Spring Batch, con persistencia en
PostgreSQL, tolerancia a fallos y escalado por particionamiento.

Los datos de entrada son los archivos oficiales de
[KariVillagran/bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data),
carpeta `data/semana_3` (1.000 filas por archivo).

---

## 1. Los tres procesos

| Job | Archivo de entrada | Tablas que produce |
|---|---|---|
| `jobReporteTransaccionesDiarias` | `transacciones.csv` | `transacciones`, `resumen_diario` |
| `jobCalculoInteresesMensuales` | `intereses.csv` | `intereses_calculados`, `cuentas` |
| `jobEstadosCuentaAnuales` | `cuentas_anuales.csv` | `movimientos_anuales`, `estado_cuenta_anual` |

Los tres siguen la misma forma: un **step particionado** que carga y valida el detalle
en paralelo, y un **step de agregación** que genera el resumen con una sola sentencia
`INSERT ... SELECT`.

```
jobReporteTransaccionesDiarias
  └─ stepLimpiarErroresTransacciones   (tasklet)
  └─ stepCargaTransaccionesMaestro     (particionado, 4 hilos)
     ├─ stepCargaTransacciones:particion-0   250 filas
     ├─ stepCargaTransacciones:particion-1   250 filas
     ├─ stepCargaTransacciones:particion-2   250 filas
     └─ stepCargaTransacciones:particion-3   250 filas
  └─ stepResumenDiario                 (tasklet: agrupa por fecha)
```

---

## 2. Cómo ejecutarlo

### Requisitos
- JDK 17
- Maven 3.9 (o el que trae NetBeans)
- PostgreSQL con una base llamada `bank_legacy_db`

```sql
CREATE DATABASE bank_legacy_db;
```

### Credenciales

No van en el código. Se leen de variables de entorno:

```bash
setx DB_URL "jdbc:postgresql://localhost:5432/bank_legacy_db"
setx DB_USER "postgres"
setx DB_PASSWORD "tu_password"
```

Las tablas las crea la aplicación sola al arrancar: las de negocio por Hibernate
(`ddl-auto=update`) y las de metadatos `BATCH_*` por Spring Batch
(`spring.batch.jdbc.initialize-schema=always`).

### Ejecución

```bash
# desde la raiz del proyecto
mvn clean package -pl batch-migration -am
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

Opciones de `--job`: `todos` (por defecto), `transacciones`, `intereses`, `anuales`.

El número de particiones se cambia sin recompilar:

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos --banco.batch.particiones=1
```

Los jobs son **idempotentes**: volver a ejecutarlos reemplaza los resultados anteriores
en vez de duplicarlos.

---

## 3. Estructura del código

```
src/main/java/com/bank/xyz/batch/
├── BatchMigrationApplication.java   arranque
├── runner/BatchRunner.java          lanza los jobs según --job
├── config/
│   ├── BatchProperties.java         chunk, particiones, hilos, topes
│   ├── InteresProperties.java       tasas de interés por tipo de cuenta
│   ├── EscaladoConfig.java          pool de hilos de las particiones
│   ├── TransaccionesJobConfig.java  proceso 1
│   ├── InteresesJobConfig.java      proceso 2
│   └── EstadoCuentaAnualJobConfig.java  proceso 3
├── dto/          filas crudas del CSV (todos los campos String)
├── model/        entidades JPA
├── processor/    validaciones y reglas de negocio
├── policy/       BancoSkipPolicy: política de descarte propia
├── partition/    RangoLineasPartitioner
├── listener/     métricas de ejecución y bitácora de errores
├── tasklet/      limpieza previa y agregaciones
└── util/         parseo de fechas, importes y normalización de texto
```

### Por qué los DTO tienen todos los campos `String`

El reader nunca convierte tipos. Lee cada fila como texto y el `ItemProcessor` decide qué
hacer con ella.

Si el reader mapeara directo a `LocalDate` o `BigDecimal`, las filas con formato raro
fallarían **dentro del reader**, antes de llegar al processor: no se podrían validar, ni
corregir, ni reportar con un motivo entendible. Leyendo como texto, el 100 % de las filas
llega al processor y cada una recibe un tratamiento explícito.

---

## 4. Los datos sucios del dataset oficial

Conteos medidos sobre las 1.000 filas de cada archivo:

**transacciones.csv**
- Cuatro formatos de fecha mezclados: `yyyy-MM-dd` (294), `dd-MM-yyyy` (250), `dd/MM/yyyy` (234), `yyyy/MM/dd` (222)
- 55 filas con `2024-13-01`, un mes que no existe
- Monto vacío en 168 filas, negativo en 141, cero en 18
- Tipo `invalid` en 308 filas y `desconocido` en 63

**intereses.csv**
- 1.000 filas pero solo **50 `cuenta_id` distintos** (101–150), cada uno repetido entre 12 y 33 veces
- Tipo `-1` en 279 filas y `unknown` en 52
- Saldo vacío en 211 filas
- Edad vacía en 193 filas y edad `150` en 52
- Nombre `Unknown` en 50 filas

**cuentas_anuales.csv**
- 20 cuentas (101–120), todas del año 2024
- `deposito` en 296 filas y `depósito` **con tilde** en 52
- Monto negativo en 254 filas, vacío en 48, cero en 12
- Descripción vacía en 230 filas

### Sobre las fechas de dos dígitos

Se interpretan como **día primero** (`dd-MM-yyyy`). No es una suposición: 302 filas tienen
el primer componente mayor que 12 y ninguna tiene el segundo mayor que 12, así que es la
única lectura consistente con el archivo.

El parseo usa `ResolverStyle.STRICT`. Con el modo por defecto, `2024-13-01` se "corrige"
en silencio a diciembre; en modo estricto falla, que es lo que se quiere para poder
reportarla como dato inválido.

---

## 5. Manejo de errores: tres tratamientos distintos

No todos los datos sucios merecen la misma reacción. El criterio es si el registro
**puede cumplir su función de negocio**.

### VÁLIDO
Pasa todas las validaciones. Se persiste tal cual.

### CORREGIDO — se transforma y se documenta
El dato tenía un problema subsanable que no impide el cálculo.

| Caso | Transformación |
|---|---|
| `depósito` con tilde | se normaliza a `deposito` quitando diacríticos |
| Monto negativo en un movimiento anual | se toma el valor absoluto |
| Edad vacía o igual a 150 | se deja en `null` |
| Descripción vacía | se completa con `SIN DESCRIPCION` |
| Nombre `Unknown` | se reemplaza por `SIN IDENTIFICAR` |

El monto negativo se normaliza porque en el estado de cuenta **el signo lo aporta el tipo
de movimiento**: un depósito suma y un retiro resta. Un monto negativo en el origen es
redundante o está mal digitado. El valor original queda registrado en `observaciones`.

La edad no entra en la fórmula de interés, así que una edad inválida no es razón para
perder el registro financiero.

### ANOMALÍA — se persiste, marcado para revisión
El registro es utilizable pero sospechoso. El enunciado pide **detectar anomalías**, así
que estos registros no se botan: se guardan con `estado = 'ANOMALIA'`.

- Monto negativo o cero en transacciones
- Tipo fuera del catálogo (`invalid`, `desconocido`), reclasificado como `no_clasificado`

### DESCARTADO — se salta y queda en `errores_batch`
El registro no puede cumplir su función. Se lanza `RegistroInvalidoException`, el step lo
salta y el listener lo anota en la tabla `errores_batch` con el motivo y la fila original.

| Proceso | Motivo del descarte | Filas |
|---|---|---:|
| Transacciones | monto ausente o no numérico | 160 |
| Transacciones | fecha inválida para un reporte diario | 55 |
| Intereses | tipo de cuenta no reconocido (`-1`) | 219 |
| Intereses | saldo ausente | 211 |
| Intereses | tipo de cuenta no reconocido (`unknown`) | 44 |
| Estados anuales | monto ausente o no numérico | 48 |

El razonamiento de cada uno:
- **Fecha inválida en transacciones**: el reporte es *diario*. Sin día válido el registro
  no se puede imputar a ninguna jornada.
- **Saldo ausente en intereses**: no hay capital sobre el cual calcular. Asumir 0
  ensuciaría el saldo final consolidado de la cuenta.
- **Tipo no reconocido en intereses**: sin tipo no hay tasa aplicable, y aplicar una por
  defecto sería inventar una condición comercial que el banco nunca pactó.

Descartar no es perder información: `errores_batch` guarda job, step, fase, motivo, la
fila original del CSV y la hora, para que auditoría pueda revisar exactamente qué no entró.

---

## 6. Política de tolerancia a fallos

`BancoSkipPolicy` distingue el **origen** del error, porque un dato sucio y una base de
datos caída no se tratan igual.

| Excepción | Decisión | Por qué |
|---|---|---|
| `RegistroInvalidoException` | se descarta, hasta 1.000 | Dato sucio conocido del legacy. Abortar el job por estos casos dejaría la migración sin correr nunca: el dataset trae cientos. |
| `ParseException` | se descarta, hasta **50** | Línea malformada. El tope es mucho más bajo a propósito: si se malforman muchas líneas, lo probable es que el archivo esté corrupto o haya cambiado de formato, y ahí conviene detenerse antes que migrar basura. |
| `DataAccessException` | **no se descarta** | Problema de infraestructura. El registro es válido y perderlo sería perder dinero. |

Complementando el descarte, los steps declaran reintentos:

```java
.retryLimit(3)
.retry(TransientDataAccessException.class)
```

Ante una falla transitoria de base de datos se reintenta el chunk en lugar de perder
registros válidos.

### La bitácora se escribe en una transacción aparte

`RegistroErroresSkipListener` inserta con `PROPAGATION_REQUIRES_NEW`. Cuando Spring Batch
descarta un item, la transacción del chunk ya viene marcada para rollback; si el error se
escribiera en esa misma transacción se perdería junto con ella, que es justo lo contrario
de lo que se busca.

---

## 7. Estrategia de escalado: particionamiento

Se eligió **particionamiento local** por sobre un step multi-hilo.

Con un step multi-hilo, todos los hilos comparten un único `ItemReader`, y
`FlatFileItemReader` no es thread-safe: hay que sincronizarlo, lo que convierte la lectura
en un cuello de botella y además rompe el reinicio, porque el estado guardado deja de ser
confiable.

Con particionamiento, cada partición tiene **su propio reader** sobre su propio rango de
líneas. No hay estado compartido, no hace falta sincronizar, y cada partición guarda su
avance por separado: si el job se cae, solo se reprocesa la partición afectada.

### Parámetros y por qué

| Parámetro | Valor | Justificación |
|---|---|---|
| `particiones` | 4 | 1.000 filas ÷ 4 = 250 por hilo, suficiente para que el paralelismo compense el costo de coordinación |
| `chunk-size` | 50 | Cada partición hace 5 commits. Equilibra memoria y cantidad de transacciones |
| `hilos-core` | 4 | Igual al número de particiones, para que arranquen todas a la vez y el paralelismo sea real y no una cola encubierta |
| `hilos-max` | 8 | Margen si se sube el número de particiones |
| `maximum-pool-size` | 20 | Por encima del pool de hilos: cada hilo toma una conexión para su chunk |

Si la cola se llena se aplica `CallerRunsPolicy`: el hilo que envía la tarea la ejecuta él
mismo. Aplica contrapresión en vez de descartar particiones.

`RangoLineasPartitioner` cuenta las filas reales del archivo y reparte rangos disjuntos.
Cuando la división no es exacta, el resto se reparte de a una entre las primeras
particiones para que ningún hilo quede desbalanceado.

Resultados medidos en `evidencias/04-comparacion-escalado.md`: **36 % menos tiempo total**
con 4 particiones frente a 1.

---

## 8. Modelo de datos

| Tabla | Contenido |
|---|---|
| `transacciones` | Detalle validado. PK = el `id` del archivo |
| `resumen_diario` | Una fila por día: total, promedio, máximo y anomalías |
| `intereses_calculados` | Una fila por registro con interés calculado |
| `cuentas` | Maestro consolidado por cuenta, con el saldo final actualizado |
| `movimientos_anuales` | Movimientos normalizados, con el año derivado de la fecha |
| `estado_cuenta_anual` | Informe por cuenta y año, con totales por tipo |
| `errores_batch` | Bitácora de cada registro descartado |

### Por qué `cuenta_id` no es la clave primaria de los intereses

`intereses.csv` trae 1.000 filas con solo 50 `cuenta_id` distintos. Usar `cuenta_id` como
`@Id` provoca violación de clave primaria a partir de la fila 51. El detalle usa clave
autogenerada y el maestro consolidado por cuenta es la tabla `cuentas`, que se calcula
después agrupando el detalle.

Al consolidar se toma el valor **más frecuente** de cada cuenta con
`mode() WITHIN GROUP`, que es más representativo que quedarse con el primero o el último
leído y, sobre todo, da un resultado estable aunque las particiones terminen en distinto
orden.

### Por qué los importes son `BigDecimal` y no `Double`

Son montos de dinero. Con `Double`, sumar 1.000 importes arrastra error de redondeo
binario y los totales del estado de cuenta anual no cuadran.

---

## 9. Cálculo de intereses

```
interes_mensual = saldo × (tasa_anual / 100) / 12
saldo_final     = saldo + interes_mensual
```

Redondeo `HALF_UP` a 2 decimales.

| Tipo de cuenta | Tasa anual | Criterio |
|---|---:|---|
| `ahorro` | 2,5 % | El banco abona al cliente |
| `hipoteca` | 8,0 % | Crédito garantizado, riesgo menor |
| `prestamo` | 12,0 % | Crédito de consumo, riesgo mayor |

El dataset no trae la tasa, así que la define el negocio. Los valores están en
`application.properties` (`banco.interes.*`) y se cambian sin tocar código.

El interés **suma** en los tres tipos: en ahorro porque el banco abona al cliente, y en
préstamo e hipoteca porque el interés devengado aumenta la deuda. El signo de la relación
—activo o pasivo— lo da el tipo de cuenta, no el saldo.

---

## 10. Pruebas

```bash
mvn test
```

32 pruebas unitarias sobre los casos sucios reales del dataset: los cuatro formatos de
fecha, el rechazo del mes 13, la unificación de `depósito` con y sin tilde, el cálculo de
interés por tipo y cada regla de descarte.

---

## 11. Evidencia de ejecución

En la carpeta [`../evidencias/`](../evidencias/):

| Archivo | Contenido |
|---|---|
| `01-ejecucion-1-particion.log` | Corrida completa con 1 partición |
| `02-ejecucion-4-particiones.log` | Corrida completa con 4 particiones |
| `03-verificacion-bd.txt` | 11 consultas SQL sobre el resultado en PostgreSQL |
| `04-comparacion-escalado.md` | Comparación de tiempos y prueba del paralelismo |

Resultado de la última corrida:

| Tabla | Filas |
|---|---:|
| `transacciones` | 785 |
| `resumen_diario` | 322 |
| `intereses_calculados` | 526 |
| `cuentas` | 50 |
| `movimientos_anuales` | 952 |
| `estado_cuenta_anual` | 20 |
| `errores_batch` | 737 |

Los totales cuadran con el origen: 1.000 filas leídas por archivo, 215 + 474 + 48 = 737
descartadas y registradas con su motivo.
