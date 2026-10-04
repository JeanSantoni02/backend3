# Arquitectura de eventos

## Patrón elegido

**Publicación/Suscripción con notificación de eventos de dominio** (publish/subscribe,
event notification), sobre **JMS** con ActiveMQ Artemis.

### Por qué este patrón y no otro

| Alternativa | Por qué se descartó |
|---|---|
| Cola punto a punto (point-to-point) | Un solo consumidor recibe cada mensaje. Hoy solo existe el servicio de notificaciones, pero el mismo retiro interesa después a antifraude y a contabilidad. Con una cola habría que duplicar la publicación por cada destino. |
| Event sourcing | Exige reconstruir el estado a partir del log de eventos y cambia el modelo de datos completo. Es desproporcionado para un sistema cuyo estado ya vive, correctamente, en tablas relacionales. |
| Event-carried state transfer | El evento llevaría una copia completa de la cuenta. Se descartó porque obliga a versionar un contrato grande y duplica información que el consumidor puede pedir si la necesita. |
| Saga / orquestación | Resuelve transacciones distribuidas entre varios servicios que escriben. Aquí solo escribe `banco-core-api`, así que no hay transacción distribuida que coordinar. |

Se eligió notificación de eventos: el mensaje lleva **lo mínimo para que el consumidor
actúe** (qué pasó, en qué cuenta, por cuánto y con qué referencia) y quien necesite más
detalle lo consulta por la API.

### Por qué JMS y no Kafka

La actividad admite ambos. Se optó por JMS con Artemis por dos razones concretas:

- El volumen es bajo y no se necesita retención ni reprocesamiento del log de eventos,
  que es lo que justifica a Kafka.
- Artemis puede correr **embebido dentro del proceso** en desarrollo, así que el sistema
  se levanta y se demuestra sin infraestructura adicional. En Docker corre como contenedor
  aparte, con la misma configuración de la aplicación.

---

## Diagrama

```
   ┌──────────────┐
   │  Cajero ATM  │
   └──────┬───────┘
          │ POST /bff/atm/cuentas/{id}/retiro
          ▼
   ┌──────────────┐        token OAuth2        ┌──────────────┐
   │   bff-atm    │◄──────────────────────────►│ auth-server  │
   │    :8083     │     client_credentials     │    :9000     │
   └──────┬───────┘                            └──────────────┘
          │ POST /api/v1/cuentas/{id}/retiros
          │ Authorization: Bearer <JWT>
          ▼
   ┌─────────────────────────────────┐
   │        banco-core-api           │        ┌──────────────┐
   │             :8080               │───────►│  PostgreSQL  │
   │                                 │  JDBC  │  bank_legacy │
   │  1. valida y bloquea la cuenta  │        └──────────────┘
   │  2. debita y confirma (COMMIT)  │
   │  3. publica el evento           │
   └───────────────┬─────────────────┘
                   │ publica
                   ▼
        ╔═══════════════════════════════════╗
        ║   TÓPICO  banco.eventos.transaccion ║   ActiveMQ Artemis
        ║   (publish / subscribe)            ║   :61616
        ╚═══════════════╤═══════════════════╝
                        │ entrega a cada suscriptor
          ┌─────────────┴──────────────┬────────────────────────┐
          ▼                            ▼                        ▼
  ┌───────────────────┐      ┌──────────────────┐   ┌──────────────────┐
  │    servicio-      │      │   antifraude     │   │   contabilidad   │
  │  notificaciones   │      │   (futuro)       │   │   (futuro)       │
  │      :8084        │      └──────────────────┘   └──────────────────┘
  │                   │
  │ genera el aviso   │
  │ al cliente        │
  └───────────────────┘
```

---

## Evento publicado

Destino: tópico `banco.eventos.transaccion`
Formato: JSON (el consumidor no depende de la clase Java del productor)

```json
{
  "eventoId": "EVT-154019",
  "tipo": "RETIRO",
  "cuentaId": 101,
  "monto": 5000,
  "saldoResultante": 103814.17,
  "referencia": "EVT-154019",
  "canal": "ATM",
  "ocurridoEn": "2026-10-04T15:40:20.09"
}
```

| Campo | Para qué lo usa el consumidor |
|---|---|
| `eventoId` | Descartar reentregas del broker |
| `tipo` | Elegir la plantilla del aviso |
| `cuentaId` | Saber a quién notificar |
| `monto`, `saldoResultante` | Componer el texto del mensaje |
| `referencia` | Cruzar con la operación en caso de reclamo |
| `canal` | Distinguir si vino del cajero, del portal o de caja |

---

## Garantías de la implementación

**El evento sale después del commit.** La publicación ocurre una vez que el débito quedó
confirmado en la base de datos. Si se publicara antes y la transacción se revirtiera, el
cliente recibiría el aviso de un retiro que nunca ocurrió.

**Publicar no puede tumbar la operación.** `PublicadorEventos` captura cualquier fallo del
broker y lo registra. El retiro ya está confirmado: perder el aviso es molesto, perder el
débito sería un error contable.

**El consumidor es idempotente.** El broker puede reentregar un mensaje. El servicio de
notificaciones lleva el registro de los `eventoId` ya procesados y descarta los repetidos,
de modo que un mismo retiro nunca genera dos avisos.

**El acoplamiento es temporal, no estructural.** Si el servicio de notificaciones está
caído, los retiros siguen funcionando: el tópico acumula y el consumidor procesa al
volver. Lo contrario (llamada síncrona al notificador) haría que una caída del correo
impidiera retirar dinero.
