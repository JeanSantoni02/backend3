# Banco XYZ — Modernización del sistema legacy

Proyecto académico de modernización del sistema legacy del Banco XYZ, en dos etapas:

- **Semana 3:** migrar tres procesos batch a **Spring Batch**, con persistencia en PostgreSQL, tolerancia a fallos y escalado por particionamiento.
- **Semana 5:** exponer esos datos mediante el patrón **Backend for Frontend (BFF)**, con un backend dedicado para cada tipo de cliente.

Los datos de entrada son los archivos oficiales de [KariVillagran/bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data), carpeta `data/semana_3` (1.000 filas por archivo).

---

## Arquitectura

```
   Navegador          App móvil         Cajero automático
       │                  │                     │
       ▼                  ▼                     ▼
   ┌────────┐        ┌──────────┐         ┌──────────┐
   │bff-web │        │bff-mobile│         │ bff-atm  │
   │ :8081  │        │  :8082   │         │  :8083   │
   └────┬───┘        └────┬─────┘         └────┬─────┘
        │                 │                    │
        └─────────────────┼────────────────────┘
                          ▼
                 ┌─────────────────┐
                 │ banco-core-api  │  único con acceso a la base
                 │     :8080       │
                 └────────┬────────┘
                          ▼
                 ┌─────────────────┐
                 │   PostgreSQL    │
                 │ bank_legacy_db  │
                 └────────┬────────┘
                          ▲
                 ┌────────┴────────┐
                 │ batch-migration │  carga los datos desde los CSV
                 └─────────────────┘
```

El batch llena la base una vez. El servicio de dominio es el único que la consulta. Los tres BFF dan forma a esos datos según lo que necesita cada cliente.

---

## Módulos

| Módulo | Puerto | Qué hace |
|---|---|---|
| [`batch-migration`](batch-migration/) | — | Tres jobs de Spring Batch que cargan y validan los CSV |
| `banco-core-api` | 8080 | Servicio de dominio. Único con acceso a PostgreSQL |
| `bff-web` | 8081 | BFF del portal: respuestas completas y agregadas |
| `bff-mobile` | 8082 | BFF de la app: respuestas mínimas |
| `bff-atm` | 8083 | BFF de cajeros: operaciones críticas, con autenticación |

---

## Cómo ejecutarlo

### Requisitos

- JDK 17
- Maven 3.9 (o el que trae NetBeans)
- PostgreSQL con la base `bank_legacy_db`

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

### 1. Compilar todo

```bash
mvn clean package
```

### 2. Cargar los datos (una sola vez)

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

Crea las tablas y carga los tres archivos. Es idempotente: se puede repetir sin duplicar.

### 3. Levantar los servicios

Cada uno en su propia terminal, **empezando por el servicio de dominio**:

```bash
java -jar banco-core-api/target/banco-core-api-1.0-SNAPSHOT.jar
```

```bash
java -jar bff-web/target/bff-web-1.0-SNAPSHOT.jar
```

```bash
java -jar bff-mobile/target/bff-mobile-1.0-SNAPSHOT.jar
```

```bash
java -jar bff-atm/target/bff-atm-1.0-SNAPSHOT.jar
```

### 4. Probar

Documentación interactiva de cada servicio:

- http://localhost:8080/swagger-ui.html — servicio de dominio
- http://localhost:8081/swagger-ui.html — BFF Web
- http://localhost:8082/swagger-ui.html — BFF Móvil
- http://localhost:8083/swagger-ui.html — BFF Cajeros

Las cuentas cargadas van de la 101 a la 150. Las que tienen movimientos y estado anual son de la 101 a la 120.

```bash
curl http://localhost:8081/bff/web/cuentas/101/panel
```

```bash
curl http://localhost:8082/bff/movil/cuentas/101
```

```bash
curl -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" http://localhost:8083/bff/atm/cuentas/101/saldo
```

Retiro desde el cajero (idempotente por el campo `referencia`):

```bash
curl -X POST http://localhost:8083/bff/atm/cuentas/101/retiro -H "Content-Type: application/json" -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" -d "{\"monto\":10000,\"referencia\":\"TICKET-001\"}"
```

> Las claves `clave-demo-001` y `clave-demo-002` son solo para probar en local. Se sobrescriben con las variables `ATM_001_KEY` y `ATM_002_KEY`.

### Pruebas

```bash
mvn test
```

53 pruebas: 32 del batch y 21 de los servicios REST.

---

## Endpoints

### banco-core-api (8080) — genérico, lo consumen los BFF

| Método | Ruta |
|---|---|
| GET | `/api/v1/cuentas` |
| GET | `/api/v1/cuentas/{id}` |
| GET | `/api/v1/cuentas/{id}/saldo` |
| GET | `/api/v1/cuentas/{id}/movimientos?anio=` |
| GET | `/api/v1/cuentas/{id}/movimientos/ultimos?cantidad=` |
| GET | `/api/v1/cuentas/{id}/estados-anuales` |
| GET | `/api/v1/cuentas/{id}/estados-anuales/{anio}` |
| GET | `/api/v1/cuentas/{id}/intereses` |
| GET | `/api/v1/transacciones/resumen-diario?desde=&hasta=` |
| POST | `/api/v1/cuentas/{id}/retiros` |

### bff-web (8081)

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/bff/web/cuentas/{id}/panel` | Todo en una llamada |
| GET | `/bff/web/cuentas` | Grilla paginada |

### bff-mobile (8082)

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/bff/movil/cuentas/{id}` | Saldo y últimos 5 movimientos |
| GET | `/bff/movil/cuentas/{id}/saldo` | Solo el saldo |

### bff-atm (8083) — requiere `X-ATM-Terminal` y `X-ATM-Key`

| Método | Ruta | Devuelve |
|---|---|---|
| GET | `/bff/atm/cuentas/{id}/saldo` | Saldo y máximo retirable |
| POST | `/bff/atm/cuentas/{id}/retiro` | Comprobante del retiro |

---

## Qué diferencia a cada BFF

Tener tres aplicaciones no es el punto del patrón. El punto es que cada una tome decisiones distintas:

| | bff-web | bff-mobile | bff-atm |
|---|---|---|---|
| Respuesta | Todo agregado | Solo lo esencial | Solo lo de la operación |
| Nombres de campo | Descriptivos | Abreviados | Descriptivos |
| Nulos | Se serializan | Se omiten | Se serializan |
| Llamadas al dominio | 4 en paralelo | 2 | 1 |
| Timeout de lectura | 5 s | 3 s | 8 s |
| Autenticación | No | No | Por terminal |

Para la misma cuenta, medido:

| BFF | Respuesta | Bytes |
|---|---|---:|
| Web | Panel completo | 3.826 |
| Móvil | Resumen | 418 |
| ATM | Saldo | 133 |
| Móvil | Solo saldo | 27 |

**El móvil transmite un 89 % menos que la web.**

El razonamiento completo —estrategias evaluadas, seguridad del cajero, integridad del retiro, códigos HTTP y resiliencia— está en [PROPUESTA-TECNICA-BFF.md](PROPUESTA-TECNICA-BFF.md).

---

## Modelo de datos

| Tabla | La llena | Contenido |
|---|---|---|
| `transacciones` | batch | Detalle validado de transacciones |
| `resumen_diario` | batch | Totales por día |
| `intereses_calculados` | batch | Interés calculado por registro |
| `cuentas` | batch | Maestro por cuenta con saldo final |
| `movimientos_anuales` | batch | Movimientos normalizados |
| `estado_cuenta_anual` | batch | Informe por cuenta y año |
| `errores_batch` | batch | Bitácora de registros descartados |
| `operaciones_atm` | core-api | Retiros, con clave de idempotencia |

---

## Evidencia

| Archivo | Contenido |
|---|---|
| [`evidencias/01-ejecucion-1-particion.log`](evidencias/) | Batch con 1 partición |
| [`evidencias/02-ejecucion-4-particiones.log`](evidencias/) | Batch con 4 particiones |
| [`evidencias/03-verificacion-bd.txt`](evidencias/) | 11 consultas SQL sobre el resultado |
| [`evidencias/04-comparacion-escalado.md`](evidencias/04-comparacion-escalado.md) | Comparación de tiempos del batch |
| [`evidencias/05-evidencia-bff.txt`](evidencias/) | Los tres BFF, seguridad, retiro y resiliencia |

Resultado de la carga:

| Tabla | Filas |
|---|---:|
| `transacciones` | 785 |
| `resumen_diario` | 322 |
| `intereses_calculados` | 526 |
| `cuentas` | 50 |
| `movimientos_anuales` | 952 |
| `estado_cuenta_anual` | 20 |
| `errores_batch` | 737 |

De 3.000 filas leídas, 737 se descartaron por datos inválidos y quedaron registradas con su motivo. El detalle de las reglas está en el [README del módulo batch](batch-migration/README.md).

---

## Documentación

- [Semana 3 — detalle del batch](batch-migration/README.md)
- [Semana 5 — propuesta técnica del BFF](PROPUESTA-TECNICA-BFF.md)
