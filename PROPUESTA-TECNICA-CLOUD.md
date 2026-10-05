# Propuesta técnica — Microservicios en la nube

Banco XYZ · Semanas 6, 7 y 8

---

## 1. El problema

Al cerrar la semana 5 el sistema ya estaba partido en cuatro servicios: el dominio y
tres BFF. Funcionaba, pero con tres defectos que impiden llevarlo a producción:

| Defecto | Consecuencia |
|---|---|
| Cada servicio guardaba su configuración en su propio jar | Cambiar un umbral obligaba a recompilar y redesplegar |
| Los BFF apuntaban al dominio por host y puerto fijos | Mover el dominio o levantar una segunda instancia rompía a los tres |
| Cualquiera que alcanzara el puerto 8080 podía retirar dinero | El endpoint crítico no distinguía quién llamaba |

A eso se suma que el dominio hacía todo de forma síncrona. Si mañana hay que avisar al
cliente de cada retiro, la opción obvia —llamar al notificador desde el retiro— haría que
una caída del correo impidiera sacar efectivo.

---

## 2. Configuración centralizada

### Alternativas evaluadas

**Opción A — Variables de entorno en cada despliegue.** Es lo que ya había. Funciona para
credenciales, pero la configuración de negocio queda repartida: para saber con qué umbral
abre el circuito hay que entrar a la máquina donde corre el BFF.

**Opción B — Spring Cloud Config con repositorio Git.** Es la forma habitual en producción:
la configuración versionada aparte del código, con historial de quién cambió qué.

**Opción C — Spring Cloud Config con perfil `native`.** El mismo servidor, leyendo de un
directorio en lugar de un repositorio remoto.

### Decisión

**Opción C.** El servidor y el contrato con los clientes son idénticos a la opción B: los
microservicios piden su configuración por HTTP y no saben de dónde sale. Cambiar a un
repositorio Git después es modificar dos líneas del `application.yml` del servidor.

Se eligió `native` porque un repositorio Git externo añade una dependencia de red que el
proyecto no necesita para demostrar el patrón, y porque el evaluador puede clonar y correr
sin configurar credenciales de otro repositorio.

### Qué quedó centralizado

| Archivo | Contenido |
|---|---|
| `application.yml` | Lo común: dirección de Eureka, actuator, formato de log |
| `banco-core-api.yml` | Base de datos, broker, issuer de OAuth2, tópico de eventos |
| `bff-atm.yml` | Umbrales del circuito, reintentos y límites del cajero |
| `servicio-notificaciones.yml` | Broker y tópico a consumir |

Los `application.yml` que quedan dentro de cada jar tienen solo tres cosas: el nombre de la
aplicación, el puerto y la dirección del config server. Todo lo demás llega de afuera.

---

## 3. Service Discovery

### Alternativas evaluadas

**Opción A — DNS y nombres de servicio.** Es lo que da Docker Compose o Kubernetes sin
agregar nada. Suficiente cuando el orquestador ya resuelve nombres.

**Opción B — Eureka.** Registro propio de la aplicación, con estado de cada instancia.

### Decisión

**Opción B.** Dos razones:

- El registro de Eureka es una vista del sistema que el DNS no da: muestra qué instancias
  hay vivas y cuándo fue el último latido de cada una. Para demostrar la arquitectura, eso
  es evidencia directa.
- La actividad pide service discovery explícitamente, y el DNS del orquestador no lo es.

Se desactivó el modo de autopreservación. En producción protege contra fallas de red masivas
—Eureka conserva registros que no responden en vez de borrarlos todos—, pero en desarrollo
hace lo contrario de lo que se necesita: deja instancias muertas en el registro durante
minutos y confunde al momento de revisar.

---

## 4. Seguridad con OAuth2

### Alternativas evaluadas

**Opción A — Clave compartida entre los BFF y el dominio.** Simple, pero una sola clave para
los tres: si se filtra la del portal, el atacante puede retirar dinero.

**Opción B — OAuth2 con `authorization_code`.** El flujo correcto cuando hay un usuario que
da consentimiento a que una aplicación actúe en su nombre.

**Opción C — OAuth2 con `client_credentials`.** El flujo para cuando el que llama es un
sistema, no una persona.

### Decisión

**Opción C.** En esta arquitectura el que llama al dominio es el BFF, no el cliente final.
El cajero ya autentica a la persona con su tarjeta y su clave; el BFF autentica a la máquina.
No hay un usuario dando consentimiento, así que `authorization_code` sobra: agregaría
pantallas de login y redirecciones que nadie va a usar.

### Lo que esto habilita

Cada BFF es un cliente distinto con sus propios scopes:

| Cliente | Scopes | Qué puede hacer |
|---|---|---|
| `bff-web` | `cuentas.leer`, `transacciones.leer` | Solo lectura |
| `bff-mobile` | `cuentas.leer` | Solo lectura de cuentas |
| `bff-atm` | `cuentas.leer`, `retiros.escribir` | Único que puede retirar |

El endpoint de retiro exige `SCOPE_retiros.escribir`. Esto resuelve el tercer defecto del
punto 1: ya no basta con alcanzar el puerto. Aunque el BFF móvil fuera comprometido y su
atacante conociera la ruta del retiro, su token no la autoriza. El daño queda acotado a lo
que ese cliente podía hacer de todos modos.

El token es un JWT firmado con RSA. El dominio valida la firma contra la clave pública del
servidor de autorización, así que no tiene que consultarle en cada petición.

---

## 5. Tolerancia a fallos

### Por qué solo en el cajero

Los tres BFF llaman al dominio, pero el del cajero es el único donde una falla tiene
consecuencia física: una persona esperando frente a una máquina que ya le pudo haber
debitado la cuenta. Un portal que no carga se recarga; un retiro a medias es un reclamo.

### Las dos decisiones que importan

**Los rechazos de negocio no son fallas.** Saldo insuficiente o cuenta inexistente están en
`ignore-exceptions`. El servicio respondió perfectamente, solo que dijo que no. Si contaran
como falla, una tanda de clientes sin fondos abriría el circuito y dejaría sin servicio a los
que sí tienen plata.

**El circuito envuelve al reintento.** Los aspectos están ordenados a propósito
(`retry-aspect-order: 2`, `circuit-breaker-aspect-order: 3`): primero se reintenta la llamada
y solo si la operación completa fracasa cuenta como un fallo del circuito. Con el orden por
omisión es al revés, y entonces un solo problema de red cuenta tres veces y abre el circuito
antes de tiempo.

Reintentar un retiro es seguro porque el dominio lo trata de forma idempotente: la misma
referencia no debita dos veces. Sin esa garantía, reintentar sería inaceptable.

| Mecanismo | Configuración |
|---|---|
| Circuit breaker | Abre con 50 % de fallos sobre una ventana de 10 llamadas, mínimo 5 |
| Espera en abierto | 20 s antes de permitir la siguiente prueba |
| Reintentos | 3 intentos con espera creciente desde 400 ms |
| Límite de tiempo | 8 s |

La transición de abierto a semiabierto es perezosa: ocurre cuando llega la siguiente llamada,
no con un temporizador. Con el temporizador activado, Resilience4j 2.1.0 entra en recursión
infinita y el circuito queda abierto para siempre.

---

## 6. Arquitectura de eventos

El patrón elegido, las alternativas descartadas, el diagrama y las garantías están en
[docs/arquitectura-eventos.md](docs/arquitectura-eventos.md).

En resumen: publicación/suscripción con notificación de eventos sobre JMS. El retiro publica
en un tópico después de confirmar el débito, y el servicio de notificaciones lo consume de
forma asíncrona. Si el notificador está caído, los retiros siguen funcionando.

---

## 7. Empaquetado y orquestación

### Imágenes

Cada servicio se construye en dos etapas: Maven compila en la primera, y la segunda copia
solo el jar sobre un JRE. La imagen final no lleva el compilador ni el código fuente, lo que
reduce su tamaño y su superficie de ataque.

El contexto de build es la raíz del repositorio, no el módulo, porque un módulo hijo no
compila sin el pom padre.

Cada imagen corre con un usuario sin privilegios. Si un servicio es comprometido, el atacante
no queda como root dentro del contenedor.

### Orquestación

El `docker-compose.yml` levanta diez componentes: la base de datos, el broker, los tres
servicios de plataforma y los cinco microservicios.

El orden importa y no se resuelve solo con `depends_on`, que únicamente espera a que el
contenedor arranque. Un PostgreSQL arrancado no es un PostgreSQL listo para aceptar
conexiones. Por eso cada dependencia usa `condition: service_healthy` contra un `healthcheck`
real: el dominio no arranca hasta que la base responde y el config server contesta su
`/actuator/health`.

Sin esto, en el primer arranque los microservicios fallan, reintentan y arrancan a destiempo.

### Diferencia entre local y contenedores

En local, el dominio levanta un broker Artemis embebido dentro del proceso, que publica un
puerto TCP para que el servicio de notificaciones se conecte igual. En Docker se usa el
contenedor de Artemis.

La diferencia está en una variable de entorno, no en el código:

```yaml
spring:
  artemis:
    mode: ${ARTEMIS_MODE:embedded}
    embedded:
      enabled: ${ARTEMIS_EMBEDDED:true}
```

Esto permite levantar y demostrar el sistema completo sin Docker instalado.

---

## 8. Resultado

| Defecto del punto 1 | Cómo quedó resuelto |
|---|---|
| Configuración dentro del jar | La sirve el config server; cambiarla no requiere recompilar |
| Direcciones fijas entre servicios | Eureka resuelve las instancias por nombre |
| Retiro accesible a cualquiera | Exige un token con `SCOPE_retiros.escribir`, que solo tiene el cajero |
| Todo síncrono | El aviso al cliente viaja por un tópico y no bloquea el retiro |

Verificado en ejecución, con la evidencia en `evidencias/08-microservicios/`:

- Los cinco microservicios registrados en Eureka.
- El dominio responde 401 sin token y 200 con token válido.
- Un retiro por el cajero que llega a PostgreSQL y genera su notificación por el tópico.
- Dos peticiones idénticas que producen un solo débito y un solo aviso.
- El ciclo completo del circuito: cerrado, abierto, semiabierto y cerrado otra vez.
