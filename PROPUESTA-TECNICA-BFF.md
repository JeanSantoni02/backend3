# Propuesta técnica — Patrón Backend for Frontend (BFF)

Banco XYZ · Semana 5

---

## 1. El problema

El Banco XYZ tiene tres clientes con necesidades incompatibles sobre los mismos datos:

| Cliente | Pantalla | Red | Qué necesita |
|---|---|---|---|
| Portal web | Grande | Fija, ancha | Mucha información a la vez: saldo, intereses, estado anual, historial paginado |
| App móvil | Chica | Celular, medida | Lo mínimo: saldo y últimos movimientos, con el menor consumo de datos posible |
| Cajero automático | Fija, sin teclado completo | Red dedicada | Dos operaciones críticas: consultar saldo y retirar efectivo, con máxima seguridad |

Con una sola API compartida hay que elegir a quién perjudicar:

- Si la API devuelve todo, el móvil descarga datos que no muestra y gasta batería y plan de datos.
- Si la API devuelve lo mínimo, la web tiene que hacer seis llamadas encadenadas para pintar una pantalla.
- Si la API expone retiros, esa operación crítica queda accesible desde el mismo endpoint que usa un navegador.

Cualquier cambio para favorecer a un cliente rompe a los otros dos. Ese acoplamiento es exactamente lo que resuelve el patrón BFF.

---

## 2. Estrategias evaluadas

### Opción A — Una API compartida para los tres clientes

Un solo backend con endpoints genéricos.

- A favor: menos código, un solo despliegue.
- En contra: es el problema descrito arriba. Además obliga a versionar la API cada vez que un cliente necesita un cambio, porque cualquier modificación afecta a todos.

**Descartada.** No es BFF y no cumple lo que pide la actividad.

### Opción B — Tres BFF, cada uno con acceso directo a PostgreSQL

Cada BFF con sus propias entidades y su propio `DataSource`.

- A favor: independencia total, menos saltos de red.
- En contra: la lógica de negocio se triplica. El cálculo del saldo, las reglas de validación y el modelo de datos quedarían copiados en tres lugares, y bastaría que uno se desactualice para que el saldo que muestra la app no coincida con el que muestra el portal. En un banco eso es inaceptable.

Además, tres aplicaciones abriendo pools de conexiones contra la misma base multiplica la carga sobre PostgreSQL sin necesidad.

**Descartada.** Duplicar reglas de dinero es un riesgo que no compensa el ahorro de un salto de red.

### Opción C — Tres BFF sobre un servicio de dominio compartido ← **elegida**

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
                 │ banco-core-api  │  ← único con acceso a la base
                 │     :8080       │
                 └────────┬────────┘
                          ▼
                 ┌─────────────────┐
                 │   PostgreSQL    │
                 │ bank_legacy_db  │
                 └────────┬────────┘
                          ▲
                 ┌────────┴────────┐
                 │ batch-migration │  ← carga los datos (semana 3)
                 └─────────────────┘
```

**Un solo lugar define las reglas** (el servicio de dominio) y **tres lugares definen la presentación** (los BFF). Cada BFF puede cambiar su contrato sin tocar a los otros dos, y el saldo se calcula una sola vez.

El costo es un salto de red adicional. Se mitiga con paralelismo en el BFF web, respuestas mínimas en el móvil y timeouts ajustados en los tres.

---

## 3. Qué hace propio cada BFF

Lo importante del patrón no es tener tres aplicaciones: es que cada una tome decisiones distintas.

| Decisión | bff-web | bff-mobile | bff-atm |
|---|---|---|---|
| Forma de la respuesta | Todo agregado en una llamada | Solo lo esencial | Solo lo necesario para la operación |
| Nombres de campo | Descriptivos | Abreviados (`f`, `t`, `m`, `d`) | Descriptivos |
| Nulos | Se serializan | Se omiten | Se serializan |
| Paginación | Sí, configurable | No: siempre 5 movimientos | No aplica |
| Llamadas al dominio | 4 en paralelo | 2 secuenciales | 1 |
| Timeout de lectura | 5 s | 3 s | 8 s |
| Autenticación | No (la resolvería el portal) | No | Credencial por terminal |
| Compresión desde | 2 KB | 512 B | — |
| Errores | Objeto con ruta y momento | Dos campos: código y mensaje | Incluye `entregarEfectivo` |

### Tamaño real de la respuesta, misma cuenta

| BFF | Respuesta | Bytes |
|---|---|---:|
| Web | Panel completo | 3.826 |
| Móvil | Resumen | 418 |
| ATM | Saldo | 133 |
| Móvil | Solo saldo | 27 |

**El móvil transmite un 89 % menos que la web** para representar la misma cuenta. Esa es la justificación medible del patrón.

### Por qué los timeouts son distintos

No es un detalle de configuración, es una decisión de negocio:

- **Móvil, 3 s:** en un celular vale más fallar rápido y dejar que la app reintente, que dejar una pantalla girando.
- **Web, 5 s:** el usuario está sentado frente a un monitor y tolera más espera que una pantalla vacía.
- **ATM, 8 s:** un retiro toma un bloqueo sobre la cuenta. Cortar la espera antes de tiempo dejaría al cajero sin saber si el débito se aplicó.

---

## 4. Seguridad del BFF de cajeros

Es el único que ejecuta operaciones sobre el dinero, así que es el único con autenticación.

**Credencial por terminal.** Cada cajero se identifica con `X-ATM-Terminal` y `X-ATM-Key` en cada petición. Sin sesiones ni cookies: es un cliente de máquina, no un navegador.

**Comparación en tiempo constante.** La clave se valida con `MessageDigest.isEqual` y no con `equals`. Con `equals`, el tiempo de respuesta varía según cuántos caracteres coinciden, y eso permite adivinar la clave carácter por carácter.

**Mensaje de error único.** Un terminal inexistente y una clave incorrecta devuelven exactamente lo mismo. Si se distinguieran, se podrían enumerar los terminales válidos probando de a uno.

**Superficie mínima.** El BFF de cajeros expone dos endpoints. Todo lo demás está denegado por defecto (`anyRequest().denyAll()`). Los otros dos BFF no exponen retiros en absoluto: pedir un retiro al BFF web devuelve 404, porque esa ruta sencillamente no existe ahí.

**Validación antes de la red.** La denominación, el mínimo y el máximo se validan en el BFF, antes de llamar al servicio de dominio. Un cajero solo entrega billetes de la denominación que tiene cargada, así que pedir 2.500 no tiene sentido físico y se rechaza de inmediato.

**Auditoría.** Cada consulta y cada retiro se registran con terminal, cuenta, monto y referencia en un logger dedicado.

---

## 5. Integridad de los datos en el retiro

El enunciado pide garantizar la integridad y consistencia. El retiro es la única operación que escribe, y tiene tres protecciones.

### Bloqueo pesimista

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Cuenta> buscarParaActualizar(Integer id);
```

Si dos cajeros retiran de la misma cuenta al mismo tiempo, el segundo espera al primero en lugar de leer un saldo ya obsoleto. Sin esto, ambos leerían el mismo saldo inicial y la cuenta podría quedar en negativo.

### Idempotencia por referencia

Cada retiro lleva una `referencia` única, con restricción `UNIQUE` en la base. Si ya existe una operación con esa referencia, se devuelve el resultado original en lugar de volver a debitar.

Esto resuelve el problema clásico del cajero: se envía el retiro, se corta la red, y el cajero no sabe si el débito se aplicó. Puede reintentar la misma referencia sin riesgo.

### El estado indeterminado

Si el servicio de dominio no responde durante un retiro, el BFF **no puede saber** si el débito se aplicó. Devolver un error genérico sería peligroso: el cajero podría interpretarlo como "no pasó nada" y dejar el dinero adentro mientras la cuenta ya fue debitada, o peor, reintentar con otra referencia y cobrar dos veces.

Por eso ese caso tiene su propia respuesta:

```json
{
  "codigo": "RETIRO_INDETERMINADO",
  "mensaje": "no se pudo confirmar la operacion, no entregue efectivo",
  "entregarEfectivo": false,
  "referencia": "ATM-001-a3f2..."
}
```

HTTP 409, `entregarEfectivo` explícito en `false`, y la referencia devuelta para reintentar o conciliar. El cajero no tiene que deducir nada del mensaje.

---

## 6. Códigos HTTP

Los códigos se eligieron para que el cliente pueda decidir qué hacer sin leer el mensaje:

| Código | Significado | Qué hace el cliente |
|---|---|---|
| 400 | Petición mal formada o monto que el cajero no puede entregar | Pedir otro monto |
| 404 | La cuenta no existe | Cancelar |
| 409 | No se pudo confirmar el retiro | **No entregar efectivo**, conciliar |
| 422 | Petición correcta que el negocio no puede cumplir (saldo insuficiente) | Ofrecer el máximo disponible |
| 503 | El servicio de dominio no responde | Reintentar más tarde |

La diferencia entre 400 y 422 importa: "pediste mal" y "no te alcanza" llevan a acciones distintas en la pantalla.

---

## 7. Resiliencia

Ningún BFF debe quedarse colgado esperando al servicio de dominio. Los tres tienen timeouts explícitos de conexión y lectura, y traducen la falla a una respuesta propia.

Con el servicio de dominio detenido, medido:

| BFF | Respuesta | Tiempo |
|---|---|---:|
| Web | 503 SERVICIO_NO_DISPONIBLE | 8 ms |
| Móvil | 503 SIN_SERVICIO | 225 ms |
| ATM (consulta) | 503 SIN_SERVICIO | 27 ms |
| ATM (retiro) | 409 RETIRO_INDETERMINADO | 9 ms |

Un detalle que costó encontrar: el BFF web lanza sus cuatro llamadas con `CompletableFuture`, y `join()` envuelve cualquier excepción en un `CompletionException`. Sin desenvolverla, el manejador de errores nunca veía la excepción real y un servicio caído se reportaba como 500 en vez de 503. Está corregido y cubierto por una prueba.

---

## 8. Por qué cada BFF define su propia copia del contrato

Los tres BFF declaran sus propios *records* para lo que reciben del servicio de dominio, en lugar de compartir un módulo común de DTOs.

Parece duplicación, y lo es a propósito. Un módulo compartido volvería a acoplar a los tres clientes: agregar un campo para la web obligaría a recompilar y volver a desplegar el móvil y el cajero. Con copias separadas, cada BFF declara solo los campos que usa (`@JsonIgnoreProperties(ignoreUnknown = true)` ignora el resto) y un cambio en el servicio de dominio solo afecta a quien realmente usa el campo que cambió.

El reflejo del contrato en el BFF móvil tiene 3 records; el de la web, 5.

---

## 9. Resumen de la decisión

Se eligió la **opción C**: tres BFF independientes sobre un servicio de dominio compartido.

- Las reglas de negocio y el acceso a datos viven en un solo lugar.
- Cada cliente recibe exactamente lo que necesita, ni más ni menos.
- La operación crítica está aislada tras autenticación en un servicio con superficie mínima.
- Los tres BFF evolucionan por separado, sin coordinación entre equipos.
