# Banco XYZ — Modernización del sistema legacy

Proyecto de la asignatura Desarrollo Backend III (PBY2203). Parte de los procesos
legacy del Banco XYZ y los lleva a una arquitectura de microservicios en la nube,
resiliente, segura y orientada a eventos.

Datos de origen: [KariVillagran/bank_legacy_data](https://github.com/KariVillagran/bank_legacy_data), carpeta `data/semana_3`.

---

## Arquitectura

```
   Navegador            App móvil           Cajero automático
       │                    │                      │
       ▼                    ▼                      ▼
  ┌─────────┐         ┌───────────┐          ┌──────────┐
  │ bff-web │         │bff-mobile │          │ bff-atm  │
  │  :8081  │         │   :8082   │          │  :8083   │
  └────┬────┘         └─────┬─────┘          └────┬─────┘
       │                    │                     │  circuit breaker + retry
       │   token OAuth2     │                     │
       └────────────────────┼─────────────────────┘
                            ▼
                   ┌──────────────────┐
                   │  banco-core-api  │──── JDBC ───► PostgreSQL
                   │      :8080       │
                   │ (resource server)│
                   └────────┬─────────┘
                            │ publica evento
                            ▼
                 ┌──────────────────────┐
                 │  ActiveMQ Artemis    │  tópico
                 │  banco.eventos.*     │  :61616
                 └──────────┬───────────┘
                            ▼
                 ┌──────────────────────┐
                 │servicio-notificaciones│
                 │        :8084          │
                 └──────────────────────┘

  Plataforma:  config-server :8888   eureka-server :8761   auth-server :9000
  Carga inicial de datos:  batch-migration (Spring Batch)
```

---

## Módulos

| Módulo | Puerto | Qué hace |
|---|---|---|
| `batch-migration` | — | Tres jobs de Spring Batch que cargan y validan los CSV |
| `config-server` | 8888 | Configuración centralizada (Spring Cloud Config) |
| `eureka-server` | 8761 | Service Discovery |
| `auth-server` | 9000 | Servidor de autorización OAuth2 |
| `banco-core-api` | 8080 | Servicio de dominio. Único con acceso a PostgreSQL |
| `servicio-notificaciones` | 8084 | Consumidor de eventos de transacción |
| `bff-web` | 8081 | BFF del portal |
| `bff-mobile` | 8082 | BFF de la app móvil |
| `bff-atm` | 8083 | BFF de cajeros, con tolerancia a fallos |

---

## Cómo ejecutarlo

### Con Docker (recomendado)

Levanta todo: base de datos, broker, plataforma y microservicios.

```bash
docker compose up -d --build
```

```bash
docker compose ps
```

```bash
docker compose logs -f banco-core-api
```

Para detener y borrar los volúmenes:

```bash
docker compose down -v
```

### Sin Docker

Requiere JDK 17, Maven y PostgreSQL con la base `bank_legacy_db`.

```bash
mvn clean package
```

Carga inicial de los datos (una sola vez):

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

Luego, un script levanta los ocho servicios en el orden que exigen sus
dependencias y espera a que cada uno responda antes de seguir con el siguiente:

```bash
powershell -ExecutionPolicy Bypass -File ejecutar-plataforma.ps1
```

```bash
powershell -ExecutionPolicy Bypass -File detener-plataforma.ps1
```

Los logs de cada servicio quedan en `logs/`.

Para levantarlos a mano, el orden es: `config-server`, `eureka-server`,
`auth-server`, `banco-core-api`, `servicio-notificaciones` y al final los tres BFF.

```bash
java -jar config-server/target/config-server-1.0-SNAPSHOT.jar
```

Sin Docker, `banco-core-api` levanta un broker Artemis **embebido** que publica un
puerto TCP en 61616, de modo que el servicio de notificaciones se conecta igual.
En Docker se usa el contenedor de Artemis.

### Credenciales

No están en el código. Se leen de variables de entorno:

```bash
setx DB_URL "jdbc:postgresql://localhost:5432/bank_legacy_db"
setx DB_USER "postgres"
setx DB_PASSWORD "tu_password"
```

Los secretos de los clientes OAuth2 (`BFF_WEB_SECRET`, `BFF_MOBILE_SECRET`,
`BFF_ATM_SECRET`) y las claves de terminal del cajero tienen valores por defecto
solo para desarrollo.

---

## Probar el sistema

Paneles:

- http://localhost:8761 — registro de Eureka
- http://localhost:8080/swagger-ui.html — servicio de dominio
- http://localhost:8081/swagger-ui.html — BFF Web
- http://localhost:8082/swagger-ui.html — BFF Móvil
- http://localhost:8083/swagger-ui.html — BFF Cajeros
- http://localhost:8084/swagger-ui.html — Notificaciones
- http://localhost:8083/actuator/circuitbreakers — estado del circuito

Obtener un token:

```bash
curl -u bff-atm:secreto-bff-atm -d "grant_type=client_credentials&scope=cuentas.leer retiros.escribir" http://localhost:9000/oauth2/token
```

Retiro que dispara un evento:

```bash
curl -X POST http://localhost:8083/bff/atm/cuentas/101/retiro -H "Content-Type: application/json" -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" -d "{\"monto\":5000,\"referencia\":\"TICKET-001\"}"
```

Ver la notificación que generó ese evento:

```bash
curl http://localhost:8084/api/v1/notificaciones
```

---

## Configuración centralizada

Ningún microservicio guarda la configuración de negocio en su propio jar. El
`config-server` la sirve desde `config-server/src/main/resources/config-repo/`:

| Archivo | Para quién |
|---|---|
| `application.yml` | Común a todos: Eureka, actuator, logging |
| `banco-core-api.yml` | Base de datos, broker, issuer OAuth2, tópico de eventos |
| `bff-atm.yml` | Umbrales del circuit breaker, reintentos y límites del cajero |
| `servicio-notificaciones.yml` | Broker y tópico a consumir |

Cambiar un umbral del circuit breaker no requiere recompilar el BFF: se edita el
`config-repo`, se reinicia el config server y el microservicio toma el valor nuevo
al arrancar.

---

## Seguridad

El `auth-server` emite tokens JWT con el flujo **client_credentials**: los BFF son
clientes de máquina, no hay un usuario final que dé consentimiento.

`banco-core-api` actúa como **resource server** y autoriza por scope:

| Cliente | Scopes | Qué puede hacer |
|---|---|---|
| `bff-web` | `cuentas.leer`, `transacciones.leer` | Solo lectura |
| `bff-mobile` | `cuentas.leer` | Solo lectura de cuentas |
| `bff-atm` | `cuentas.leer`, `retiros.escribir` | Único que puede retirar |

El endpoint de retiro exige `SCOPE_retiros.escribir`, así que aunque el BFF móvil
conociera la ruta, su token no se lo permitiría.

El BFF de cajeros suma una segunda capa: cada terminal se identifica con sus
cabeceras `X-ATM-Terminal` y `X-ATM-Key`, validadas en tiempo constante.

---

## Tolerancia a fallos

`bff-atm` protege sus llamadas al dominio con Resilience4j:

| Mecanismo | Configuración |
|---|---|
| Circuit breaker | Abre con 50 % de fallos sobre una ventana de 10 llamadas, mínimo 5 |
| Espera en abierto | 20 s antes de permitir la siguiente prueba |
| Reintentos | 3 intentos con espera creciente desde 400 ms |
| Límite de tiempo | 8 s |

Los rechazos de negocio (saldo insuficiente, cuenta inexistente) están en
`ignore-exceptions`: son respuestas correctas del servicio, no fallas, y no deben
abrir el circuito.

El orden de los aspectos está fijado para que **el circuito envuelva al reintento**:
primero se reintenta la llamada y solo si la operación completa falla cuenta como
fallo del circuito. Con el orden por defecto es al revés.

Reintentar un retiro es seguro porque el dominio lo trata de forma idempotente: la
misma `referencia` no debita dos veces.

---

## Arquitectura de eventos

Un retiro confirmado publica un evento en el tópico `banco.eventos.transaccion`.
El servicio de notificaciones lo consume de forma asíncrona.

El detalle del patrón elegido, las alternativas descartadas, el diagrama y las
garantías están en [docs/arquitectura-eventos.md](docs/arquitectura-eventos.md).

Tres decisiones que vale la pena destacar:

- El evento sale **después** del commit, para no avisar de un retiro que podría revertirse.
- Publicar no puede tumbar la operación: un fallo del broker se registra y se sigue.
- El consumidor descarta reentregas por `eventoId`, así un retiro nunca genera dos avisos.

---

## Docker

Cada microservicio tiene su `Dockerfile` en dos etapas: compila con Maven y ejecuta
sobre un JRE, con un usuario sin privilegios. La caché del repositorio local se
comparte entre imágenes con `--mount=type=cache`.

El `docker-compose.yml` orquesta los diez componentes con `healthcheck` y
`depends_on: condition: service_healthy`, de modo que los microservicios no
arrancan antes de que su infraestructura esté lista.

---

## Pruebas

```bash
mvn test
```

---

## Evidencia

| Carpeta | Contenido |
|---|---|
| [`evidencias/08-microservicios/`](evidencias/08-microservicios/) | Config server, Eureka, OAuth2, eventos JMS, resiliencia, los tres BFF y la orquestación |
| [`evidencias/06-apis-consola/`](evidencias/06-apis-consola/) | Las cuatro APIs del patrón BFF |
| `evidencias/01` a `04` | Ejecución del batch y comparación de escalado |

Se regenera con los servicios levantados:

```bash
powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-microservicios.ps1
```

---

## Documentación

- [Arquitectura de eventos](docs/arquitectura-eventos.md)
- [Detalle del batch](batch-migration/README.md)
- [Propuesta técnica del patrón BFF](PROPUESTA-TECNICA-BFF.md)
