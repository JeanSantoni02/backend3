# -*- coding: utf-8 -*-
"""
Arma el informe de evidencia en PDF juntando las capturas de 07-capturas/ y las
salidas de consola de 06-apis-consola/.

Genera primero un HTML con las imagenes embebidas en base64 y despues lo
imprime a PDF con Chrome en modo headless. Se hace asi para no depender de
librerias externas de Python: Chrome ya esta instalado y maqueta mejor.

Uso, parado en la raiz del proyecto:
    python evidencias\\generar-informe-pdf.py

Requiere haber ejecutado antes generar-evidencia-apis.ps1 y generar-capturas.ps1
"""

import base64
import datetime
import html
import os
import subprocess
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
CAPTURAS = os.path.join(AQUI, '07-capturas')
CONSOLA = os.path.join(AQUI, '06-apis-consola')
SALIDA_HTML = os.path.join(AQUI, 'informe-evidencia.html')
SALIDA_PDF = os.path.join(AQUI, 'Evidencia-de-ejecucion-Banco-XYZ.pdf')

NAVEGADORES = [
    r'C:\Program Files\Google\Chrome\Application\chrome.exe',
    r'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe',
    os.path.expandvars(r'%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe'),
    r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
]


def imagen(nombre, pie=''):
    ruta = os.path.join(CAPTURAS, nombre)
    if not os.path.exists(ruta):
        return '<p class="falta">Falta la captura %s</p>' % html.escape(nombre)
    with open(ruta, 'rb') as f:
        datos = base64.b64encode(f.read()).decode('ascii')
    leyenda = '<figcaption>%s</figcaption>' % html.escape(pie) if pie else ''
    return ('<figure><img src="data:image/png;base64,%s" alt="%s">%s</figure>'
            % (datos, html.escape(nombre), leyenda))


def leer_consola(archivo):
    ruta = os.path.join(CONSOLA, archivo)
    if not os.path.exists(ruta):
        return ''
    with open(ruta, encoding='utf-8-sig') as f:
        return f.read()


def bloque(texto):
    return '<pre>%s</pre>' % html.escape(texto.rstrip())


def recortar(texto, desde, hasta=None):
    """Devuelve el tramo del archivo entre dos marcadores."""
    i = texto.find(desde)
    if i == -1:
        return ''
    resto = texto[i:]
    if hasta:
        j = resto.find(hasta, len(desde))
        if j != -1:
            resto = resto[:j]
    return resto.rstrip()


ESTILOS = """
@page { size: A4; margin: 16mm 14mm 16mm 14mm; }
* { box-sizing: border-box; }
body { font-family: "Segoe UI", Calibri, Arial, sans-serif; color: #1c2230;
       font-size: 10.5pt; line-height: 1.5; margin: 0; }
h1 { font-size: 21pt; margin: 0 0 6px; letter-spacing: -0.3px; }
h2 { font-size: 15pt; margin: 0 0 4px; padding-bottom: 6px;
     border-bottom: 2px solid #1f4e79; color: #1f4e79; }
h3 { font-size: 11.5pt; margin: 18px 0 6px; color: #23496e; }
p { margin: 6px 0; }
.portada { text-align: left; padding-top: 32mm; }
.portada .sub { font-size: 13pt; color: #4a5568; margin-top: 2px; }
.portada .meta { margin-top: 26mm; font-size: 10pt; color: #4a5568; }
.portada .meta td { padding: 3px 14px 3px 0; vertical-align: top; }
.portada .meta td:first-child { color: #8a94a6; white-space: nowrap; }
.pagina { page-break-before: always; }
figure { margin: 10px 0 14px; page-break-inside: avoid; }
img { width: 100%; border: 1px solid #ccd3de; border-radius: 3px; display: block; }
figcaption { font-size: 8.5pt; color: #5a6478; margin-top: 4px; font-style: italic; }
pre { background: #f6f8fa; border: 1px solid #dde3ec; border-left: 3px solid #1f4e79;
      border-radius: 3px; padding: 8px 10px; font-family: Consolas, "Courier New", monospace;
      font-size: 7.3pt; line-height: 1.35; white-space: pre-wrap; word-break: break-word;
      margin: 8px 0; page-break-inside: avoid; }
table.datos { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9.5pt; }
table.datos th { background: #1f4e79; color: #fff; text-align: left; padding: 5px 8px; }
table.datos td { border-bottom: 1px solid #dde3ec; padding: 5px 8px; }
table.datos td.num { text-align: right; font-variant-numeric: tabular-nums; }
.nota { background: #fdf6e3; border-left: 3px solid #d8a13a; padding: 7px 10px;
        margin: 10px 0; font-size: 9.5pt; page-break-inside: avoid; }
.arq { font-family: Consolas, "Courier New", monospace; font-size: 8pt; line-height: 1.25;
       background: #f6f8fa; border: 1px solid #dde3ec; padding: 10px; white-space: pre; }
.falta { color: #b02a37; font-size: 9pt; }
.indice li { margin: 3px 0; }
"""


def construir_html():
    hoy = datetime.date.today().strftime('%d-%m-%Y')

    atm = leer_consola('04-bff-atm.txt')
    core = leer_consola('01-banco-core-api.txt')
    web = leer_consola('02-bff-web.txt')
    movil = leer_consola('03-bff-mobile.txt')
    comparativa = leer_consola('05-comparativa-bff.txt')

    partes = []
    a = partes.append

    a('<!doctype html><html lang="es"><head><meta charset="utf-8">')
    a('<title>Evidencia de ejecucion - Banco XYZ</title>')
    a('<style>%s</style></head><body>' % ESTILOS)

    # ---------------- Portada ----------------
    a('<div class="portada">')
    a('<h1>Evidencia de ejecución</h1>')
    a('<div class="sub">Modernización del sistema legacy del Banco XYZ</div>')
    a('<div class="sub">Patrón Backend for Frontend sobre procesos Spring Batch</div>')
    a('<table class="meta">')
    a('<tr><td>Autor</td><td>Jean Luc Santoni</td></tr>')
    a('<tr><td>Repositorio</td><td>github.com/JeanSantoni02/backend3</td></tr>')
    a('<tr><td>Fecha</td><td>%s</td></tr>' % hoy)
    a('<tr><td>Contenido</td><td>Capturas de pantalla y salidas de consola de las cuatro APIs</td></tr>')
    a('</table>')
    a('</div>')

    # ---------------- Introduccion ----------------
    a('<div class="pagina"><h2>1. Sistema evaluado</h2>')
    a('<p>El sistema tiene cuatro servicios. Un proceso batch carga los archivos '
      'oficiales del Banco XYZ en PostgreSQL. Un servicio de dominio es el único '
      'que consulta esa base. Tres BFF dan forma a esos datos según lo que '
      'necesita cada tipo de cliente.</p>')
    a('<div class="arq">'
      '   Navegador          App móvil         Cajero automático\n'
      '       |                  |                     |\n'
      '       v                  v                     v\n'
      '   +--------+        +----------+         +----------+\n'
      '   |bff-web |        |bff-mobile|         | bff-atm  |\n'
      '   | :8081  |        |  :8082   |         |  :8083   |\n'
      '   +----+---+        +----+-----+         +----+-----+\n'
      '        |                 |                    |\n'
      '        +-----------------+--------------------+\n'
      '                          v\n'
      '                 +-----------------+\n'
      '                 | banco-core-api  |  único con acceso a la base\n'
      '                 |     :8080       |\n'
      '                 +--------+--------+\n'
      '                          v\n'
      '                 +-----------------+\n'
      '                 |   PostgreSQL    |\n'
      '                 | bank_legacy_db  |\n'
      '                 +--------+--------+\n'
      '                          ^\n'
      '                 +--------+--------+\n'
      '                 | batch-migration |  carga los datos desde los CSV\n'
      '                 +-----------------+</div>')

    a('<h3>Datos cargados por el batch</h3>')
    a('<table class="datos"><tr><th>Tabla</th><th>Filas</th><th>Contenido</th></tr>'
      '<tr><td>transacciones</td><td class="num">785</td><td>Detalle validado</td></tr>'
      '<tr><td>resumen_diario</td><td class="num">322</td><td>Totales por día</td></tr>'
      '<tr><td>intereses_calculados</td><td class="num">526</td><td>Interés por registro</td></tr>'
      '<tr><td>cuentas</td><td class="num">50</td><td>Maestro con saldo final</td></tr>'
      '<tr><td>movimientos_anuales</td><td class="num">952</td><td>Movimientos normalizados</td></tr>'
      '<tr><td>estado_cuenta_anual</td><td class="num">20</td><td>Informe por cuenta y año</td></tr>'
      '<tr><td>errores_batch</td><td class="num">737</td><td>Registros descartados con su motivo</td></tr>'
      '</table>')
    a('<p>De 3.000 filas leídas, 737 se descartaron por datos inválidos y quedaron '
      'registradas con el motivo del descarte.</p>')
    a('</div>')

    # ---------------- API 1 ----------------
    a('<div class="pagina"><h2>2. banco-core-api (puerto 8080)</h2>')
    a('<p>Servicio de dominio. Es el único componente con acceso a PostgreSQL y '
      'expone endpoints genéricos sobre las tablas que dejó el batch. Los tres '
      'BFF lo consumen; ningún cliente final lo usa directamente.</p>')
    a(imagen('01-swagger-banco-core-api.png',
             'Documentación interactiva del servicio de dominio en http://localhost:8080/swagger-ui.html'))
    a(imagen('05-core-api-cuenta.png', 'GET /api/v1/cuentas/101 — detalle de la cuenta'))
    a(imagen('06-core-api-estado-anual.png',
             'GET /api/v1/cuentas/101/estados-anuales — salida del proceso 3 del batch'))
    a('</div>')

    a('<div class="pagina">')
    a(imagen('07-core-api-resumen-diario.png',
             'GET /api/v1/transacciones/resumen-diario — salida del proceso 1 del batch'))
    a(imagen('12-core-api-error-404.png',
             'GET /api/v1/cuentas/999 — el error se devuelve estructurado, nunca un stacktrace'))
    a('<h3>Salida de consola</h3>')
    a(bloque(recortar(core, '--- Detalle de una cuenta ---', '--- Movimientos')))
    a(bloque(recortar(core, '--- Estados de cuenta anuales', '--- Intereses')))
    a('</div>')

    # ---------------- API 2 ----------------
    a('<div class="pagina"><h2>3. bff-web (puerto 8081)</h2>')
    a('<p>Backend dedicado al portal de navegador. Resuelve en paralelo cuatro '
      'consultas al servicio de dominio y devuelve en una sola respuesta todo lo '
      'que la pantalla necesita, para que el navegador no tenga que encadenar '
      'llamadas.</p>')
    a(imagen('02-swagger-bff-web.png', 'Documentación interactiva del BFF Web'))
    a(imagen('08-bff-web-panel.png',
             'GET /bff/web/cuentas/101/panel — titular, saldo, estado anual, intereses y movimientos en una sola respuesta'))
    a('<h3>Salida de consola</h3>')
    a(bloque(recortar(web, '--- Panel completo', '--- Filtrado por anio')))
    a('</div>')

    # ---------------- API 3 ----------------
    a('<div class="pagina"><h2>4. bff-mobile (puerto 8082)</h2>')
    a('<p>Backend dedicado a la app de teléfono. Devuelve lo mínimo: saldo y '
      'últimos cinco movimientos, con nombres de campo cortos, sin campos nulos '
      'y con las descripciones acortadas, para reducir el consumo de datos.</p>')
    a(imagen('03-swagger-bff-mobile.png', 'Documentación interactiva del BFF Móvil'))
    a(imagen('09-bff-movil-resumen.png',
             'GET /bff/movil/cuentas/101 — los movimientos usan f (fecha), t (tipo), m (monto) y d (descripción)'))
    a(imagen('10-bff-movil-saldo.png',
             'GET /bff/movil/cuentas/101/saldo — 27 bytes para la consulta más frecuente de la app'))
    a('<h3>Salida de consola</h3>')
    a(bloque(recortar(movil, '--- Pantalla principal', '--- Consulta de saldo')))
    a('</div>')

    # ---------------- API 4 ----------------
    a('<div class="pagina"><h2>5. bff-atm (puerto 8083)</h2>')
    a('<p>Backend dedicado a los cajeros automáticos. Es el único que ejecuta '
      'operaciones sobre el dinero, por lo tanto el único con autenticación: cada '
      'terminal se identifica en cada petición con su credencial.</p>')
    a(imagen('04-swagger-bff-atm.png',
             'Los candados y el botón Authorize indican que ambos endpoints exigen credencial de terminal'))
    a(imagen('11-bff-atm-sin-credencial.png',
             'GET sin credencial — la petición se rechaza con 401 antes de llegar al controlador'))
    a('<div class="nota"><b>Por qué el mensaje de error es siempre el mismo:</b> un '
      'terminal inexistente y una clave incorrecta devuelven exactamente la misma '
      'respuesta. Si se distinguieran, se podrían enumerar los terminales válidos '
      'probando de a uno.</div>')
    a('</div>')

    a('<div class="pagina"><h3>5.1 Seguridad</h3>')
    a(bloque(recortar(atm, '--- Sin credencial ---', '####')))
    a('<h3>5.2 Retiro e idempotencia</h3>')
    a('<p>Esta es la prueba más importante del sistema: el mismo retiro se envía '
      'dos veces con la misma referencia y el saldo baja una sola vez.</p>')
    a(bloque(recortar(atm, '--- Consulta de saldo con credencial valida ---', '####')))
    a('</div>')

    a('<div class="pagina"><h3>5.3 Validaciones de negocio</h3>')
    a(bloque(recortar(atm, '--- Monto que no es multiplo')))
    a('</div>')

    # ---------------- Comparativa ----------------
    a('<div class="pagina"><h2>6. Comparación entre los tres BFF</h2>')
    a('<p>Tener tres aplicaciones no es el punto del patrón. El punto es que cada '
      'una tome decisiones distintas sobre los mismos datos.</p>')
    a('<table class="datos">'
      '<tr><th></th><th>bff-web</th><th>bff-mobile</th><th>bff-atm</th></tr>'
      '<tr><td>Respuesta</td><td>Todo agregado</td><td>Solo lo esencial</td><td>Solo lo de la operación</td></tr>'
      '<tr><td>Nombres de campo</td><td>Descriptivos</td><td>Abreviados</td><td>Descriptivos</td></tr>'
      '<tr><td>Campos nulos</td><td>Se serializan</td><td>Se omiten</td><td>Se serializan</td></tr>'
      '<tr><td>Llamadas al dominio</td><td>4 en paralelo</td><td>2</td><td>1</td></tr>'
      '<tr><td>Timeout de lectura</td><td>5 s</td><td>3 s</td><td>8 s</td></tr>'
      '<tr><td>Autenticación</td><td>No</td><td>No</td><td>Por terminal</td></tr>'
      '</table>')
    a(bloque(recortar(comparativa, '--- Tamano de la respuesta ---', '--- Respuesta del BFF web')))
    a('<div class="nota">El BFF móvil transmite cerca de un 90&nbsp;% menos que el '
      'BFF web para representar la misma cuenta. Esa es la justificación medible '
      'del patrón.</div>')
    a('</div>')

    a('</body></html>')
    return '\n'.join(partes)


def main():
    if not os.path.isdir(CAPTURAS) or not os.path.isdir(CONSOLA):
        print('Faltan las carpetas de evidencia. Ejecuta primero:')
        print('  powershell -ExecutionPolicy Bypass -File evidencias\\generar-evidencia-apis.ps1')
        print('  powershell -ExecutionPolicy Bypass -File evidencias\\generar-capturas.ps1')
        return 1

    with open(SALIDA_HTML, 'w', encoding='utf-8') as f:
        f.write(construir_html())
    print('HTML intermedio: %s' % SALIDA_HTML)

    navegador = next((n for n in NAVEGADORES if os.path.exists(n)), None)
    if not navegador:
        print('No se encontro Chrome ni Edge para imprimir el PDF.')
        return 1

    perfil = os.path.join(os.environ.get('TEMP', '.'), 'chrome-informe-banco')
    comando = [
        navegador,
        '--headless=new',
        '--disable-gpu',
        '--no-first-run',
        '--no-pdf-header-footer',
        '--user-data-dir=%s' % perfil,
        '--virtual-time-budget=30000',
        '--print-to-pdf=%s' % SALIDA_PDF,
        'file:///%s' % SALIDA_HTML.replace('\\', '/'),
    ]
    subprocess.run(comando, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=False)

    if os.path.exists(SALIDA_PDF):
        kb = os.path.getsize(SALIDA_PDF) // 1024
        print('PDF generado: %s  (%d KB)' % (SALIDA_PDF, kb))
        os.remove(SALIDA_HTML)
        return 0

    print('No se pudo generar el PDF.')
    return 1


if __name__ == '__main__':
    sys.exit(main())
