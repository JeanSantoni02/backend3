# Instrucciones para ejecutar y probar el sistema

Guía paso a paso para levantar la plataforma del Banco XYZ y verificar cada componente.
Los comandos están escritos para **PowerShell en Windows**; al final de cada sección se
indica la diferencia en Linux o macOS cuando la hay.

---

## 1. Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | 17 | Compilar y ejecutar los servicios |
| Maven | 3.9 o superior | Construir el proyecto multimódulo |
| PostgreSQL | 14 o superior | Base de datos, si se ejecuta sin Docker |
| Docker Desktop | 4.x con Compose v2 | Solo para la ejecución en contenedores |
| curl | incluido en Windows 10/11 | Probar los endpoints |

Puertos que deben estar libres:

| Puerto | Servicio | Puerto | Servicio |
|---|---|---|---|
| 8888 | config-server | 8085 | servicio-clientes |
| 8761 | eureka-server | 8086 | servicio-cuentas |
| 9000 | auth-server | 8087, 8088 | servicio-pagos (dos instancias) |
| 8080 | banco-core-api | 8081 | bff-web |
| 8084 | servicio-notificaciones | 8082 | bff-mobile |
| 61616 | broker Artemis | 8083 | bff-atm |
| 5432 | PostgreSQL | **8443** | **api-gateway (HTTPS)** |

---

## 2. Obtener y compilar

```bash
git clone https://github.com/JeanSantoni02/backend3.git
```

```bash
cd backend3
```

```bash
mvn clean package
```

Compila los 13 módulos y ejecuta las 82 pruebas unitarias. Debe terminar con
`BUILD SUCCESS`.

---

## 3. Base de datos

Crear la base vacía (solo la primera vez):

```bash
psql -U postgres -c "CREATE DATABASE bank_legacy_db;"
```

Definir la contraseña para la sesión actual:

```bash
$env:DB_PASSWORD = "tu_password"
```

En Linux o macOS: `export DB_PASSWORD=tu_password`.

Si PostgreSQL está en otro host o puerto, definir también `DB_URL`
(por defecto `jdbc:postgresql://localhost:5432/bank_legacy_db`) y `DB_USER`.

---

## 4. Componente 1 — Migración batch

Ejecuta los tres procesos que reemplazan al sistema legacy, en orden:

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

También se puede correr uno solo con `--job=transacciones`, `--job=intereses` o
`--job=anuales`.

### Qué verificar

En la consola, cada job debe terminar en `COMPLETED`:

```
FIN JOB: jobReporteTransaccionesDiarias | estado: COMPLETED
TOTAL leidas=1000 escritas=1114 descartadas=211
FIN JOB: jobCalculoInteresesMensuales | estado: COMPLETED
TOTAL leidas=1000 escritas=591 descartadas=459
FIN JOB: jobEstadosCuentaAnuales | estado: COMPLETED
TOTAL leidas=1000 escritas=972 descartadas=48
```

Cada fila descartada queda registrada con su motivo:

```bash
psql -U postgres -d bank_legacy_db -c "SELECT job_nombre, motivo, count(*) FROM errores_batch GROUP BY 1,2 ORDER BY 1,3 DESC;"
```

### Probar la reejecución automática

La prueba unitaria simula un job que falla y verifica que se relanza con los mismos
parámetros, de modo que Spring Batch lo reanuda desde el step que falló:

```bash
mvn -pl batch-migration test -Dtest=BatchRunnerTest
```

Para verlo con la base real: detener PostgreSQL a mitad de una ejecución. El log muestra
los reintentos ante la caída de conexión y, si persiste, la reejecución:

```
WARN  BatchRunner : El job jobReporteTransaccionesDiarias fallo. Reejecucion automatica 1/2 en 2000 ms
```

> **El batch va antes que los servicios** cuando la base está vacía. Algunos servicios
> mapean las mismas tablas en modo solo lectura y, si las crean ellos primero, no generan
> las secuencias que el batch necesita.

---

## 5. Levantar la plataforma

### Opción A — Script (recomendada)

Levanta los 13 procesos en el orden de sus dependencias y espera a que cada uno responda
antes de seguir:

```bash
powershell -ExecutionPolicy Bypass -File ejecutar-plataforma.ps1
```

Si `DB_PASSWORD` no está definida, la pide. Los logs quedan en `logs/`. Para detener todo:

```bash
powershell -ExecutionPolicy Bypass -File detener-plataforma.ps1
```

### Opción B — Manual

Cada comando en su propia terminal, en este orden, esperando a que el anterior muestre
`Started ...Application`:

```bash
java -jar config-server/target/config-server-1.0-SNAPSHOT.jar
```

```bash
java -jar eureka-server/target/eureka-server-1.0-SNAPSHOT.jar
```

```bash
java -jar auth-server/target/auth-server-1.0-SNAPSHOT.jar
```

```bash
java -jar banco-core-api/target/banco-core-api-1.0-SNAPSHOT.jar
```

Después `servicio-notificaciones`, `servicio-clientes`, `servicio-cuentas`,
`servicio-pagos`, los tres BFF y al final `api-gateway`.

Para la segunda instancia de pagos, en otra terminal:

```bash
$env:PORT = "8088"; java -jar servicio-pagos/target/servicio-pagos-1.0-SNAPSHOT.jar
```

Sin Docker, `banco-core-api` levanta un broker Artemis embebido en el puerto 61616, al que
se conectan los demás servicios.

### Opción C — Docker Compose

Ver la sección 13.

---

## 6. Componente 2 — Plataforma Spring Cloud

### Config Server

```bash
curl http://localhost:8888/servicio-pagos/default
```

Devuelve la configuración que el servidor entrega a `servicio-pagos`: la propia
(`servicio-pagos.yml`) más la compartida (`application.yml`).

### Eureka

Abrir http://localhost:8761. Deben aparecer los nueve servicios en estado `UP`, y
`SERVICIO-PAGOS` con **dos** instancias.

### Servidor de autorización

```bash
curl -u portal-operaciones:secreto-portal-operaciones -d "grant_type=client_credentials&scope=clientes.leer clientes.escribir cuentas.leer cuentas.escribir pagos.leer pagos.escribir" http://localhost:9000/oauth2/token
```

Responde un `access_token` (JWT). Para los pasos siguientes conviene guardarlo:

```bash
$T = (curl.exe -s -u portal-operaciones:secreto-portal-operaciones -d "grant_type=client_credentials&scope=clientes.leer clientes.escribir cuentas.leer cuentas.escribir pagos.leer pagos.escribir" http://localhost:9000/oauth2/token | ConvertFrom-Json).access_token
```

Clientes registrados y lo que puede hacer cada uno:

| Cliente | Scopes |
|---|---|
| `portal-operaciones` | todos los de clientes, cuentas y pagos |
| `servicio-cuentas` | `clientes.leer` |
| `bff-web` | `cuentas.leer`, `transacciones.leer` |
| `bff-mobile` | `cuentas.leer` |
| `bff-atm` | `cuentas.leer`, `retiros.escribir` |

---

## 7. Componente 3 — API Gateway (HTTPS)

Todas las llamadas de esta sección y las siguientes pasan por https://localhost:8443.
El certificado es autofirmado: `curl` necesita `-k` y el navegador mostrará una
advertencia.

| Prueba | Comando | Resultado esperado |
|---|---|---|
| Responde por HTTPS | `curl -k https://localhost:8443/actuator/health` | `{"status":"UP"}` |
| No atiende HTTP plano | `curl http://localhost:8443/actuator/health` | Sin respuesta |
| Exige token | `curl -k https://localhost:8443/api/v1/clientes` | HTTP 401 |
| Valida el scope | depositar con un token de `bff-mobile` | HTTP 403 |

Rutas que expone:

| Ruta | Servicio |
|---|---|
| `/api/v1/clientes/**` | servicio-clientes |
| `/api/v1/cuentas/**` | servicio-cuentas |
| `/api/v1/pagos/**` | servicio-pagos |
| `/api/v1/notificaciones/**`, `/api/v1/alertas/**` | servicio-notificaciones |
| `/bff/web/**`, `/bff/movil/**`, `/bff/atm/**` | los tres BFF |

---

## 8. Componente 4 — Gestión de clientes

Para no repetir el token en cada comando:

```bash
$H = "Authorization: Bearer $T"
```

Listar:

```bash
curl -k -H $H https://localhost:8443/api/v1/clientes
```

Crear (el cuerpo va en un archivo porque PowerShell altera las comillas):

```bash
'{"rut":"12345670-K","nombre":"Camila Rojas","email":"camila@correo.cl"}' | Set-Content cliente.json
```

```bash
curl -k -H $H -H "Content-Type: application/json" --data-binary "@cliente.json" https://localhost:8443/api/v1/clientes
```

| Caso | Resultado esperado |
|---|---|
| RUT válido y nuevo | HTTP 201 con el cliente creado |
| El mismo RUT otra vez | HTTP 409 |
| RUT con dígito verificador incorrecto | HTTP 400 |

Swagger: http://localhost:8085/swagger-ui.html

---

## 9. Componente 5 — Gestión de cuentas

Abrir una cuenta para el cliente 1:

```bash
'{"clienteId":1,"tipo":"ahorro","depositoInicial":50000}' | Set-Content cuenta.json
```

```bash
curl -k -H $H -H "Content-Type: application/json" --data-binary "@cuenta.json" https://localhost:8443/api/v1/cuentas
```

Responde HTTP 201 con un número de cuenta desde 100000. Los números menores corresponden a
las cuentas migradas por el batch.

| Operación | Comando |
|---|---|
| Ver una cuenta | `curl -k -H $H https://localhost:8443/api/v1/cuentas/100000` |
| Cuentas de un cliente | `curl -k -H $H "https://localhost:8443/api/v1/cuentas?clienteId=1"` |
| Bloquear | `PATCH /api/v1/cuentas/100000` con `{"estado":"BLOQUEADA"}` |
| Cerrar | `POST /api/v1/cuentas/100000/cierre` |

El cierre solo procede con saldo cero; si no, responde HTTP 422 `SALDO_PENDIENTE`.

Swagger: http://localhost:8086/swagger-ui.html

---

## 10. Componente 6 — Procesamiento de pagos

```bash
'{"cuentaOrigen":100000,"cuentaDestino":101,"monto":15000,"referencia":"TRF-001"}' | Set-Content trf.json
```

```bash
curl -k -H $H -H "Content-Type: application/json" --data-binary "@trf.json" https://localhost:8443/api/v1/pagos/transferencias
```

| Endpoint | Cuerpo |
|---|---|
| `POST /api/v1/pagos/depositos` | `{"cuentaId":101,"monto":20000,"referencia":"DEP-001"}` |
| `POST /api/v1/pagos/transferencias` | `{"cuentaOrigen":..,"cuentaDestino":..,"monto":..,"referencia":".."}` |
| `POST /api/v1/pagos/servicios` | `{"cuentaId":101,"monto":12990,"referencia":"PAG-001","convenio":"Electricidad"}` |
| `GET /api/v1/pagos/{referencia}` | — |
| `GET /api/v1/pagos?cuentaId=101` | — |

| Caso | Resultado esperado |
|---|---|
| Primera vez | HTTP 201 |
| Misma referencia otra vez | HTTP 200 con `"duplicado":true`, sin volver a debitar |
| Sin saldo suficiente | HTTP 422 `SALDO_INSUFICIENTE` |
| Cuenta bloqueada o cerrada | HTTP 422 `CUENTA_NO_OPERATIVA` y una alerta de seguridad |
| Monto de 1.000.000 o más | Se aplica y genera una alerta `MONTO_INUSUAL` |

Swagger: http://localhost:8087/swagger-ui.html

### Probar el balanceo de carga

Repetir varias veces la misma consulta y mirar la cabecera `X-Atendido-Por`:

```bash
1..6 | ForEach-Object { curl.exe -sk -D - -o NUL -H $H "https://localhost:8443/api/v1/pagos?cuentaId=101" | Select-String "X-Atendido-Por" }
```

Debe alternar entre `:8087` y `:8088`.

---

## 11. Componente 7 — Eventos y alertas

Cada operación confirmada publica un evento en el tópico `banco.eventos.transaccion`.
Las alertas viajan por `banco.eventos.alertas`. `servicio-notificaciones` consume ambos:

```bash
curl http://localhost:8084/api/v1/notificaciones
```

```bash
curl http://localhost:8084/api/v1/alertas
```

### Probar la tolerancia a fallos

1. Detener `servicio-clientes` (cerrar su terminal o terminar el proceso del puerto 8085).
2. Intentar abrir una cuenta varias veces (paso 9).
3. Cada intento responde HTTP 503 con un mensaje claro, en vez de quedar esperando.
4. Revisar el estado del circuito:

```bash
curl http://localhost:8086/actuator/circuitbreakers
```

Después de cinco llamadas fallidas el circuito queda `OPEN` y deja de intentar la llamada.
Al volver a levantar `servicio-clientes`, pasados 15 segundos, la siguiente apertura
prueba de nuevo y el circuito se cierra.

---

## 12. Componente 8 — Los tres BFF

Siguen funcionando como en la semana 5, ahora también a través del gateway:

```bash
curl -k https://localhost:8443/bff/web/cuentas/101/panel
```

```bash
curl -k https://localhost:8443/bff/movil/cuentas/101
```

```bash
curl -k -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" https://localhost:8443/bff/atm/cuentas/101/saldo
```

---

## 13. Ejecución con Docker Compose

Con la base de un contenedor nuevo, primero se carga el batch:

```bash
$env:DB_HOST_PORT = "5433"; docker compose up -d postgres
```

```bash
$env:DB_URL = "jdbc:postgresql://localhost:5433/bank_legacy_db"; $env:DB_PASSWORD = "postgres"
```

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

```bash
docker compose up -d --build
```

```bash
docker compose ps
```

Los quince contenedores deben quedar `healthy` (`servicio-pagos` corre con dos réplicas).
`DB_HOST_PORT` solo hace falta si ya hay un PostgreSQL ocupando el 5432 en la máquina.

En contenedores, solo el gateway (8443) es la entrada prevista: los servicios de clientes,
cuentas y pagos no publican puertos.

Para detener:

```bash
docker compose down
```

Con `docker compose down -v` se borra también el volumen de la base.

---

## 14. Evidencia automática

Con la plataforma arriba, estos scripts recorren todo lo anterior y dejan el resultado en
`evidencias/09-eft/`:

```bash
powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-eft.ps1
```

| Archivo | Contenido |
|---|---|
| `01-batch-fin-legacy-data.txt` | Ejecución de los tres jobs |
| `02-microservicios.txt` | Descubrimiento, HTTPS, OAuth2, los tres servicios, balanceo, eventos, alertas y tolerancia a fallos |
| `03-escalado-batch.txt` | Tiempos del batch con una y con cuatro particiones |
| `04-equivalencia-particiones.txt` | Sumas de control idénticas con una y con cuatro particiones |
| `capturas/` | Eureka, Swagger de cada servicio, alertas, notificaciones y estado del circuito |

---

## 15. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| Un servicio no arranca: `password authentication failed` | Falta `DB_PASSWORD` | Definirla antes de lanzar |
| `Port 8085 was already in use` | Quedó un proceso anterior | `detener-plataforma.ps1` |
| El batch falla con `null value in column "id"` | Los servicios crearon las tablas antes | Correr el batch con la base vacía, antes de los servicios |
| 401 con un token recién emitido | El `auth-server` se reinició y cambió su clave | Pedir un token nuevo |
| El navegador rechaza https://localhost:8443 | Certificado autofirmado | Aceptar la excepción; es de desarrollo |
| La apertura de cuentas responde 503 | `servicio-clientes` caído o circuito abierto | Levantarlo y esperar 15 s |
