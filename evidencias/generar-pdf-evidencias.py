import base64, html, io, os, subprocess, sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EVID = os.path.join(RAIZ, "evidencias", "08-microservicios")
CAPS = os.path.join(EVID, "capturas")
SALIDA_HTML = os.path.join(EVID, "_evidencias.html")
SALIDA_PDF = os.path.join(RAIZ, "evidencias",
                          "Evidencias Semana 8 Jean Santoni Backend 3.pdf")

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
if not os.path.exists(CHROME):
    CHROME = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"


def imagen(nombre):
    ruta = os.path.join(CAPS, nombre)
    with open(ruta, "rb") as f:
        return "data:image/jpeg;base64," + base64.b64encode(f.read()).decode()


def consola(nombre, maximo=None):
    with open(os.path.join(EVID, nombre), encoding="utf-8") as f:
        t = f.read().replace("\ufeff", "")
    lineas = t.split("\n")
    recortado = maximo is not None and len(lineas) > maximo
    if recortado:
        lineas = lineas[:maximo]
    salida = html.escape("\n".join(lineas).rstrip())
    if recortado:
        salida += "\n\n[...]  La salida completa esta en el archivo " + nombre
    return salida


CAPTURAS = [
    ("01-eureka-servicios-registrados.jpg",
     "Service Discovery",
     "Los cinco microservicios registrados en Eureka y en estado UP. El aviso rojo "
     "confirma que la autopreservacion esta desactivada, de modo que una instancia "
     "caida desaparece del registro en vez de quedar colgada."),
    ("02-config-server-sirviendo-bff-atm.jpg",
     "Configuracion centralizada",
     "El config server respondiendo la configuracion de bff-atm. Entrega dos fuentes: "
     "bff-atm.yml con los umbrales del circuito y los reintentos, y application.yml con "
     "lo comun a todos los servicios. Nada de esto vive dentro del jar del BFF."),
    ("03-swagger-servicio-dominio.jpg",
     "Servicio de dominio",
     "La API del dominio, unico modulo con acceso a PostgreSQL. El endpoint de retiro "
     "esta marcado como operacion critica e idempotente por el campo referencia."),
    ("04-swagger-bff-cajeros.jpg",
     "BFF de cajeros",
     "Superficie minima: solo saldo y retiro. Los candados indican que ambas operaciones "
     "exigen la credencial de terminal, ademas del token OAuth2 que el BFF usa hacia el dominio."),
    ("07-swagger-bff-web.jpg",
     "BFF del portal",
     "Respuestas completas para el navegador: el endpoint de panel arma en una sola "
     "llamada lo que la pantalla necesita."),
    ("08-swagger-bff-movil.jpg",
     "BFF movil",
     "El mismo dominio, pero devolviendo solo lo esencial para reducir el consumo de datos."),
    ("09-swagger-notificaciones.jpg",
     "Servicio de notificaciones",
     "El consumidor de eventos. No lo llama nadie: se alimenta del topico JMS."),
    ("05-circuit-breaker-estado.jpg",
     "Estado del circuit breaker",
     "El actuator exponiendo la instancia coreApi en estado CLOSED, con su umbral de "
     "fallos en 50 % y el conteo de llamadas de la ventana."),
    ("10-eureka-en-contenedores.jpg",
     "La misma plataforma, en contenedores",
     "Eureka corriendo dentro de Docker. Los identificadores de instancia son los "
     "hostname de cada contenedor, no los de la maquina: los cinco microservicios se "
     "descubren por la red interna que creo docker-compose."),
    ("06-notificacion-por-evento.jpg",
     "Notificacion generada por un evento",
     "La notificacion que produjo el servicio consumidor a partir del evento que publico "
     "el dominio tras confirmar el retiro. El eventoId coincide con la referencia del retiro."),
]

CONSOLAS = [
    ("01-config-server-y-eureka.txt",
     "Configuracion centralizada y Service Discovery", None),
    ("02-oauth2.txt",
     "OAuth2: tokens, scopes y rechazo sin credencial", None),
    ("03-eventos-jms.txt",
     "Arquitectura de eventos: publicacion, consumo e idempotencia", None),
    ("04-resiliencia.txt",
     "Tolerancia a fallos: ciclo completo del circuit breaker", None),
    ("05-bff-sobre-la-plataforma.txt",
     "Los tres BFF operando sobre la plataforma", None),
    ("06-docker-compose.txt",
     "Dockerizacion: validacion del archivo", 40),
    ("07-docker-en-ejecucion.txt",
     "Orquestacion: los diez contenedores en ejecucion", None),
]

partes = []
partes.append("""<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Evidencias Semana 8</title>
<style>
  @page { size: A4; margin: 18mm 15mm; }
  * { box-sizing: border-box; }
  body { font-family: "Segoe UI", Arial, sans-serif; color: #1a1a1a; font-size: 10.5pt;
         line-height: 1.5; margin: 0; }
  .portada { height: 240mm; display: flex; flex-direction: column;
             justify-content: center; page-break-after: always; }
  .portada .linea { width: 70px; height: 5px; background: #0b5d3b; margin-bottom: 26px; }
  .portada h1 { font-size: 30pt; margin: 0 0 6px; letter-spacing: -0.5px; }
  .portada h2 { font-size: 15pt; font-weight: 400; color: #555; margin: 0 0 44px; }
  .portada dl { margin: 0; font-size: 11pt; }
  .portada dt { color: #888; font-size: 8.5pt; text-transform: uppercase;
                letter-spacing: 1px; margin-top: 16px; }
  .portada dd { margin: 2px 0 0; font-weight: 600; }
  h3 { font-size: 15pt; margin: 0 0 4px; color: #0b5d3b;
       border-bottom: 2px solid #0b5d3b; padding-bottom: 6px; }
  h4 { font-size: 11.5pt; margin: 0 0 3px; }
  section { page-break-before: always; }
  section.seguida { page-break-before: auto; }
  .bloque { page-break-inside: avoid; margin-bottom: 20px; }
  .bloque p { margin: 0 0 8px; color: #444; font-size: 9.5pt; }
  img { width: 100%; border: 1px solid #ccc; display: block; }
  pre { background: #1e1e1e; color: #e4e4e4; padding: 11px 13px; font-size: 6.4pt;
        line-height: 1.42; font-family: Consolas, monospace; white-space: pre-wrap;
        word-break: break-word; border-radius: 3px; margin: 0; }
  table { width: 100%; border-collapse: collapse; font-size: 9.5pt; margin: 10px 0 18px; }
  th { background: #0b5d3b; color: #fff; text-align: left; padding: 7px 9px; font-size: 9pt; }
  td { border-bottom: 1px solid #ddd; padding: 6px 9px; vertical-align: top; }
  td.mono { font-family: Consolas, monospace; font-size: 8.5pt; white-space: nowrap; }
  .intro { color: #444; margin-bottom: 16px; }
  .nota { background: #fdf6e3; border-left: 4px solid #d4a017; padding: 10px 13px;
          font-size: 9.5pt; margin: 14px 0; }
</style></head><body>""")

partes.append("""
<div class="portada">
  <div class="linea"></div>
  <h1>Evidencias de ejecucion</h1>
  <h2>Microservicios y resiliencia en la nube con Spring Cloud</h2>
  <dl>
    <dt>Estudiante</dt><dd>Jean Luc Santoni Camilo</dd>
    <dt>Asignatura</dt><dd>Desarrollo Backend III &mdash; PBY2203</dd>
    <dt>Actividad</dt><dd>Experiencia 3, Semana 8</dd>
    <dt>Proyecto</dt><dd>Modernizacion del sistema legacy del Banco XYZ</dd>
    <dt>Repositorio</dt><dd>github.com/JeanSantoni02/backend3</dd>
  </dl>
</div>""")

partes.append("""
<section class="seguida">
<h3>1. Que se implemento</h3>
<p class="intro">El proyecto parte de los procesos batch del Banco XYZ y los lleva a una
arquitectura de microservicios segura, resiliente y orientada a eventos. Esta entrega cubre
los tres puntos pedidos para la semana 8 sobre la base construida en las semanas 6 y 7.</p>
<table>
  <tr><th style="width:22%">Requerimiento</th><th>Como se resolvio</th></tr>
  <tr><td><b>OAuth2.0</b></td><td>Servidor de autorizacion propio que emite JWT firmados con RSA
      mediante el flujo <i>client_credentials</i>. El servicio de dominio actua como resource
      server y autoriza por scope: solo el BFF de cajeros puede retirar.</td></tr>
  <tr><td><b>Imagenes Docker</b></td><td>Ocho Dockerfile en dos etapas. Maven compila en la
      primera y la segunda copia solo el jar sobre un JRE, con un usuario sin privilegios.</td></tr>
  <tr><td><b>docker-compose</b></td><td>Orquesta diez componentes con healthcheck y
      <i>condition: service_healthy</i>, de modo que ningun microservicio arranca antes que
      su infraestructura. Verificado: los diez quedan en estado <i>healthy</i>.</td></tr>
</table>
<table>
  <tr><th style="width:30%">Modulo</th><th style="width:14%">Puerto</th><th>Funcion</th></tr>
  <tr><td class="mono">config-server</td><td class="mono">8888</td><td>Configuracion centralizada</td></tr>
  <tr><td class="mono">eureka-server</td><td class="mono">8761</td><td>Service Discovery</td></tr>
  <tr><td class="mono">auth-server</td><td class="mono">9000</td><td>Servidor de autorizacion OAuth2</td></tr>
  <tr><td class="mono">banco-core-api</td><td class="mono">8080</td><td>Dominio. Unico con acceso a PostgreSQL</td></tr>
  <tr><td class="mono">servicio-notificaciones</td><td class="mono">8084</td><td>Consumidor de eventos de transaccion</td></tr>
  <tr><td class="mono">bff-web</td><td class="mono">8081</td><td>BFF del portal</td></tr>
  <tr><td class="mono">bff-mobile</td><td class="mono">8082</td><td>BFF de la app movil</td></tr>
  <tr><td class="mono">bff-atm</td><td class="mono">8083</td><td>BFF de cajeros, con tolerancia a fallos</td></tr>
  <tr><td class="mono">batch-migration</td><td class="mono">&mdash;</td><td>Carga inicial de los datos (semana 3)</td></tr>
</table>
</section>""")

partes.append('<section><h3>2. Capturas de la plataforma en ejecucion</h3>'
              '<p class="intro">Los ocho servicios levantados en la maquina local, '
              'con los datos cargados por el batch en PostgreSQL.</p>')
for archivo, titulo, texto in CAPTURAS:
    partes.append(
        '<div class="bloque"><h4>%s</h4><p>%s</p><img src="%s" alt=""></div>'
        % (html.escape(titulo), html.escape(texto), imagen(archivo)))
partes.append("</section>")

partes.append('<section><h3>3. Salidas de consola</h3>'
              '<p class="intro">Generadas con el script '
              '<span style="font-family:Consolas">evidencias\\generar-evidencia-microservicios.ps1</span>, '
              'que consulta los servicios en ejecucion y escribe los resultados sin intervencion manual.</p>')
for archivo, titulo, maximo in CONSOLAS:
    partes.append('<div class="bloque"><h4>%s</h4><pre>%s</pre></div>'
                  % (html.escape(titulo), consola(archivo, maximo)))
partes.append("""
<div class="nota"><b>Sobre la base de datos en contenedores.</b> El volumen de
PostgreSQL arranca vacio, asi que la carga inicial se hace una sola vez corriendo el
job de batch contra el puerto que publica el contenedor. Por eso la consulta de una
cuenta responde 404 con un token valido: la autorizacion paso y la peticion llego a
la consulta, pero la cuenta aun no esta cargada.</div>
</section>""")

partes.append("</body></html>")

with io.open(SALIDA_HTML, "w", encoding="utf-8") as f:
    f.write("\n".join(partes))

print("html: %s (%.1f MB)" % (SALIDA_HTML, os.path.getsize(SALIDA_HTML) / 1048576))

if os.path.exists(SALIDA_PDF):
    os.remove(SALIDA_PDF)

subprocess.run([
    CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
    "--print-to-pdf=" + SALIDA_PDF, "--virtual-time-budget=20000",
    "file:///" + SALIDA_HTML.replace("\\", "/")
], check=True, capture_output=True)

if os.path.exists(SALIDA_PDF):
    print("pdf:  %s (%.2f MB)" % (SALIDA_PDF, os.path.getsize(SALIDA_PDF) / 1048576))
    os.remove(SALIDA_HTML)
else:
    print("no se genero el pdf", file=sys.stderr)
    sys.exit(1)
