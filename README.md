# Banco XYZ — Modernización del sistema legacy

Proyecto de la asignatura Desarrollo Backend III (PBY2203), Duoc UC.

El Banco XYZ opera hace treinta años sobre un mainframe con procesos en COBOL y scripts
Shell. Este proyecto lo lleva a una arquitectura de microservicios preparada para la nube:
migra los procesos batch a Spring Batch, separa un backend por canal con el patrón BFF y
construye los servicios de dominio con Spring Cloud, seguros, resilientes y comunicados
por eventos.

Datos de origen: [KariVillagran/fin_legacy_data](https://github.com/KariVillagran/fin_legacy_data).

| Documento | Contenido |
|---|---|
| [instrucciones.md](instrucciones.md) | Cómo ejecutar y probar cada componente, paso a paso |
| [despliegue.md](despliegue.md) | Cómo desplegar en AWS: EC2 con Compose o ECS Fargate |
| [PROPUESTA-TECNICA-CLOUD.md](PROPUESTA-TECNICA-CLOUD.md) | Decisiones de configuración, descubrimiento, seguridad y resiliencia |
| [PROPUESTA-TECNICA-BFF.md](PROPUESTA-TECNICA-BFF.md) | Decisiones del patrón Backend for Frontend |
| [docs/arquitectura-eventos.md](docs/arquitectura-eventos.md) | Patrón de eventos, tópicos y garantías |
| [batch-migration/README.md](batch-migration/README.md) | Detalle de los tres procesos batch |

---

## Arquitectura

```
     Navegador        App móvil        Cajero        Backoffice de operaciones
         │                │               │                    │
         └────────────────┴───────┬───────┴────────────────────┘
                                  │ HTTPS
                                  ▼
                       ┌─────────────────────┐
                       │     api-gateway     │  TLS, enrutamiento,
                       │        :8443        │  balanceo, circuit breaker
                       └──────────┬──────────┘
          ┌──────────┬───────────┼────────────┬─────────────┬──────────┐
          ▼          ▼           ▼            ▼             ▼          ▼
      bff-web   bff-mobile    bff-atm    servicio-     servicio-   servicio-
       :8081      :8082        :8083     clientes      cuentas     pagos
          │          │           │        :8085         :8086    :8087 :8088
          └──────────┼───────────┘          ▲             │  │       │
                     ▼                      └─── valida ──┘  │       │
              banco-core-api                     al titular  │       │
                  :8080                                      │       │
                     │          ┌────────────────────────────┴───────┘
                     ▼          ▼          publica eventos y alertas
               ┌───────────────────┐       ┌──────────────────────────┐
               │    PostgreSQL     │       │  ActiveMQ Artemis :61616 │
               └─────────▲─────────┘       │  banco.eventos.transaccion│
                         │                 │  banco.eventos.alertas    │
                  batch-migration          └────────────┬─────────────┘
                  (Spring Batch)                        ▼
                                              servicio-notificaciones :8084

   Plataforma:  config-server :8888 · eureka-server :8761 · auth-server :9000 (OAuth2)
```

---

## Módulos

| Módulo | Puerto | Responsabilidad |
|---|---|---|
| `batch-migration` | — | Tres jobs de Spring Batch que reemplazan a los procesos legacy |
| `config-server` | 8888 | Configuración centralizada |
| `eureka-server` | 8761 | Descubrimiento de servicios |
| `auth-server` | 9000 | Servidor de autorización OAuth2 |
| `api-gateway` | 8443 | Entrada única por HTTPS, enrutamiento y balanceo de carga |
| `servicio-clientes` | 8085 | Información personal y perfiles de clientes |
| `servicio-cuentas` | 8086 | Apertura, cierre y mantenimiento de cuentas |
| `servicio-pagos` | 8087, 8088 | Depósitos, transferencias y pagos; publica eventos y alertas |
| `servicio-notificaciones` | 8084 | Consumidor de eventos y alertas |
| `banco-core-api` | 8080 | Consultas y retiros que usan los BFF |
| `bff-web` | 8081 | Backend del portal: respuestas completas |
| `bff-mobile` | 8082 | Backend de la app: respuestas livianas |
| `bff-atm` | 8083 | Backend de cajeros: autenticación por terminal y tolerancia a fallos |

---

## Inicio rápido

```bash
mvn clean package
```

```bash
java -jar batch-migration/target/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

```bash
powershell -ExecutionPolicy Bypass -File ejecutar-plataforma.ps1
```

O con contenedores:

```bash
docker compose up -d --build
```

El detalle, los requisitos y cómo probar cada componente están en
[instrucciones.md](instrucciones.md).

---

## Parte 1 — Migración batch

| Proceso legacy | Job | Resultado sobre `fin_legacy_data` |
|---|---|---|
| Reporte de transacciones diarias | `jobReporteTransaccionesDiarias` | 789 conservadas (397 marcadas como anomalía), 211 descartadas, resumen de 325 días |
| Cálculo de intereses | `jobCalculoInteresesMensuales` | 541 cálculos, 459 descartados, 50 cuentas consolidadas |
| Estados de cuenta anuales | `jobEstadosCuentaAnuales` | 952 movimientos, 48 descartados, 20 estados de cuenta |

| Requerimiento | Cómo se resolvió |
|---|---|
| Leer, procesar y escribir | Cada job tiene un step de lectura CSV, un procesador que valida y normaliza, y un writer JPA |
| Excepciones y reintentos | `faultTolerant()` con hasta 3 reintentos ante fallas transitorias de la base y omisión de registros inválidos, que quedan en `errores_batch` con su motivo |
| Grandes volúmenes | Step particionado en 4 hilos con chunks de 50 filas |
| Finalización y reejecución | Si un job termina en `FAILED`, se relanza automáticamente con los mismos parámetros hasta dos veces; Spring Batch lo reanuda desde el step que falló. `startLimit` impide reintentos indefinidos |
| Integridad | Cada job recarga completo su resultado; el de intereses solo reconstruye las cuentas migradas y nunca toca las abiertas en línea |

Un monto negativo o un tipo fuera de catálogo no se descartan: se conservan marcados como
anomalía, porque detectarlas es justamente el objetivo del reporte diario. Solo se descarta
lo que no se puede ubicar en el tiempo o no tiene monto.

---

## Parte 2 — Backend for Frontend

| Canal | BFF | Autenticación | Lo que lo distingue |
|---|---|---|---|
| Portal web | `bff-web` | Token OAuth2 hacia el dominio | Arma la pantalla completa en una sola llamada |
| App móvil | `bff-mobile` | Token OAuth2 hacia el dominio | Responde solo lo esencial: 418 bytes contra 3826 del web |
| Cajero | `bff-atm` | Clave por terminal más token OAuth2 | Solo saldo y retiro; circuit breaker, reintentos e idempotencia |

Los tres son independientes: cada uno se despliega y evoluciona por separado. Las
comunicaciones externas van por HTTPS a través del gateway.

---

## Parte 3 — Microservicios con Spring Cloud

| Requerimiento | Cómo se resolvió |
|---|---|
| Gestión de cuentas | `servicio-cuentas`: apertura con validación del titular, mantenimiento y cierre con saldo cero |
| Procesamiento de pagos | `servicio-pagos`: depósitos, transferencias y pagos, con bloqueo pesimista e idempotencia por referencia |
| Gestión de clientes | `servicio-clientes`: alta con RUT validado por dígito verificador, perfil y estado |
| Configuración | Spring Cloud Config; cada servicio solo trae su nombre y puerto |
| Descubrimiento | Eureka |
| Balanceo y enrutamiento | Spring Cloud Gateway con rutas `lb://` sobre Spring Cloud LoadBalancer; `servicio-cuentas` llama a `servicio-clientes` con un `RestClient` balanceado |
| OAuth2 | Servidor de autorización propio; cada servicio valida el JWT y autoriza por scope |
| Resilience4j | Circuit breaker y reintentos en `bff-atm` y en `servicio-cuentas`; circuit breaker por ruta en el gateway |
| Comportamiento alternativo | Respuestas 503 claras en lugar de esperas: la cuenta no se abre si no se puede confirmar al titular |
| Tópicos | `banco.eventos.transaccion` para operaciones completadas y `banco.eventos.alertas` para alertas de seguridad |
| Productores y consumidores | `servicio-pagos`, `servicio-cuentas` y `banco-core-api` publican; `servicio-notificaciones` consume |
| Nube | Imágenes Docker y procedimiento de despliegue en AWS ([despliegue.md](despliegue.md)) |
| Orquestación | `docker-compose.yml` con 15 contenedores y healthchecks |

### Consideraciones

**Consistencia.** Una transferencia debita y abona en la misma transacción, bloqueando las
dos cuentas siempre en el mismo orden para que dos transferencias cruzadas no se esperen
mutuamente. Los eventos se publican recién después del commit, y los consumidores descartan
reentregas.

**Monitoreo.** Cada servicio expone salud y métricas en formato Prometheus, y cada línea de
log lleva el `traceId` de la petición, que viaja de un servicio a otro.

**Escalado horizontal.** `servicio-pagos` corre con dos instancias. Una instancia nueva se
registra sola en Eureka y el gateway le empieza a enviar tráfico sin configuración
adicional.

---

## Pruebas

```bash
mvn test
```

82 pruebas unitarias: procesadores y reejecución del batch, validación de RUT, reglas de
apertura y cierre de cuentas, y reglas de pagos (saldo, bloqueos, idempotencia, alertas y
orden de bloqueo).

---

## Evidencia

| Carpeta | Contenido |
|---|---|
| [`evidencias/09-eft/`](evidencias/09-eft/) | Batch sobre `fin_legacy_data` y recorrido completo de la plataforma |
| [`evidencias/09-eft/capturas/`](evidencias/09-eft/capturas/) | Eureka, Swagger, alertas, notificaciones y estado del circuito |
| [`evidencias/08-microservicios/`](evidencias/08-microservicios/) | Semana 8: OAuth2, eventos, resiliencia y Docker |
| [`evidencias/06-apis-consola/`](evidencias/06-apis-consola/) | Semana 5: los BFF |

Se regenera con la plataforma arriba:

```bash
powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-eft.ps1
```
