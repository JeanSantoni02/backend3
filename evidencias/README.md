# Evidencia de ejecución

Salidas de consola y capturas de pantalla de la ejecución del batch y de las cuatro APIs.

---

## Qué hay en esta carpeta

### Semana 3 — procesos batch

| Archivo | Contenido |
|---|---|
| `01-ejecucion-1-particion.log` | Corrida completa de los tres jobs con 1 partición |
| `02-ejecucion-4-particiones.log` | La misma corrida con 4 particiones |
| `03-verificacion-bd.txt` | 11 consultas SQL sobre lo que quedó en PostgreSQL |
| `04-comparacion-escalado.md` | Comparación de tiempos y prueba de que el paralelismo se ejercita |

### Semana 5 — APIs

| Archivo | Contenido |
|---|---|
| `05-evidencia-bff.txt` | Recorrido general por los tres BFF |
| `06-apis-consola/01-banco-core-api.txt` | Servicio de dominio: consultas y manejo de errores |
| `06-apis-consola/02-bff-web.txt` | BFF Web: panel agregado, filtros y paginación |
| `06-apis-consola/03-bff-mobile.txt` | BFF Móvil: respuestas mínimas |
| `06-apis-consola/04-bff-atm.txt` | BFF Cajeros: seguridad, retiro idempotente y validaciones |
| `06-apis-consola/05-comparativa-bff.txt` | La misma cuenta vista por los tres BFF, con tamaños |
| `07-capturas/*.png` | 12 capturas de Swagger UI y de las respuestas |

Cada bloque de `06-apis-consola/` muestra la petición completa (método, URL, cabeceras y cuerpo), el código HTTP, el tiempo de respuesta y el JSON devuelto.

---

## Cómo regenerar la evidencia

### Requisito previo

Los cuatro servicios deben estar corriendo. Desde la raíz del proyecto, cada uno en su terminal:

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

### Salidas de consola

```bash
powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-apis.ps1
```

Ejecuta todos los casos contra las cuatro APIs y reescribe los `.txt` de `06-apis-consola/`. Verifica primero que los cuatro servicios respondan y avisa si falta alguno.

### Capturas de pantalla

```bash
powershell -ExecutionPolicy Bypass -File evidencias\generar-capturas.ps1
```

Usa Chrome en modo headless con un perfil temporal aparte: no abre ninguna ventana ni toca tu navegador ni lo que tengas en pantalla. Reescribe los PNG de `07-capturas/`.

### Evidencia del batch

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos --banco.batch.particiones=4 > evidencias/02-ejecucion-4-particiones.log 2>&1
```

Cambia `--banco.batch.particiones=1` para la comparación.

---

## Capturas que hay que tomar a mano

Dos casos no se pueden automatizar con un navegador headless, porque necesitan enviar cabeceras o un cuerpo POST.

### Retiro desde el cajero

1. Abre http://localhost:8083/swagger-ui.html
2. Botón **Authorize** (arriba a la derecha).
3. En `terminal` escribe `ATM-001`, en `clave` escribe `clave-demo-001`. Authorize y Close.
4. Despliega `POST /bff/atm/cuentas/{cuentaId}/retiro` → **Try it out**.
5. `cuentaId`: `101`. Cuerpo:

```json
{ "monto": 10000, "referencia": "TICKET-001" }
```

6. **Execute**. Captura la pantalla: se ve la petición enviada, el código 200 y el comprobante.
7. **Vuelve a ejecutar exactamente lo mismo, sin cambiar la referencia.** Captura de nuevo: el saldo resultante es el mismo y `duplicado` pasa a `true`. Esa segunda captura es la que demuestra la idempotencia, que es lo más difícil de mostrar.

### Consulta de saldo autenticada

Mismo procedimiento con `GET /bff/atm/cuentas/{cuentaId}/saldo`, que devuelve el saldo y el máximo retirable ya calculado.

> Para capturar la pantalla en Windows: `Win + Shift + S` recorta un área y la copia; `Win + Impr Pant` guarda la pantalla completa en `Imágenes\Capturas de pantalla`.

---

## Qué demuestra cada cosa

| Requisito | Dónde se ve |
|---|---|
| Las tres APIs de cliente funcionan | `06-apis-consola/02`, `03`, `04` y capturas `02`, `03`, `04` |
| Cada BFF devuelve algo distinto | `06-apis-consola/05-comparativa-bff.txt` |
| Los datos vienen del batch | Capturas `06` y `07`: estado anual y resumen diario |
| El cajero está protegido | `06-apis-consola/04`, sección Seguridad, y captura `11` |
| El retiro no cobra dos veces | `06-apis-consola/04`, sección Operaciones |
| Los errores se manejan | `06-apis-consola/04`, sección Validaciones, y captura `12` |
| El sistema resiste caídas | `05-evidencia-bff.txt`, sección 4 |
