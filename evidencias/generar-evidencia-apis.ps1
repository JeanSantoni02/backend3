# ============================================================================
#  Genera la evidencia de ejecucion de las cuatro APIs del Banco XYZ.
#
#  Requisitos: los cuatro servicios deben estar corriendo.
#      java -jar banco-core-api/target/banco-core-api-1.0-SNAPSHOT.jar
#      java -jar bff-web/target/bff-web-1.0-SNAPSHOT.jar
#      java -jar bff-mobile/target/bff-mobile-1.0-SNAPSHOT.jar
#      java -jar bff-atm/target/bff-atm-1.0-SNAPSHOT.jar
#
#  Uso, parado en la raiz del proyecto:
#      powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-apis.ps1
#
#  Deja un archivo .txt por API en evidencias\06-apis-consola\
# ============================================================================

$ErrorActionPreference = 'Stop'

$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path $PSScriptRoot '06-apis-consola'
if (-not (Test-Path $destino)) { New-Item -ItemType Directory -Path $destino | Out-Null }

$ATM_TERMINAL = 'ATM-001'
$ATM_KEY = 'clave-demo-001'

# ---------------------------------------------------------------------------
#  Ejecuta una peticion con curl.exe y devuelve codigo HTTP + cuerpo.
#  Se usa curl.exe y no Invoke-WebRequest porque pasa los bytes sin
#  reinterpretarlos, y asi las tildes de las descripciones no se rompen.
# ---------------------------------------------------------------------------
function Invoke-Api {
    param(
        [string]$Metodo = 'GET',
        [string]$Url,
        [string[]]$Cabeceras = @(),
        [string]$Cuerpo = $null
    )

    $argumentos = @('-s', '-S', '--max-time', '15', '-w', "`n___HTTP___%{http_code}___%{time_total}")
    $argumentos += @('-X', $Metodo)
    foreach ($c in $Cabeceras) { $argumentos += @('-H', $c) }

    # El cuerpo JSON va por archivo temporal y no como argumento directo.
    # PowerShell 5.1 reescribe las comillas al pasar argumentos a un ejecutable
    # nativo, asi que {"monto":10000} llega a curl como {monto:10000} y el
    # servidor recibe un JSON invalido.
    $temporal = $null
    if ($Cuerpo) {
        $temporal = [System.IO.Path]::GetTempFileName()
        [System.IO.File]::WriteAllText($temporal, $Cuerpo, (New-Object System.Text.UTF8Encoding($false)))
        $argumentos += @('-H', 'Content-Type: application/json', '--data-binary', "@$temporal")
    }
    $argumentos += $Url

    try {
        $salida = (& curl.exe @argumentos 2>&1) -join "`n"
    } finally {
        if ($temporal -and (Test-Path $temporal)) { Remove-Item $temporal -Force }
    }

    $codigo = '???'
    $tiempo = '?'
    $cuerpo = $salida
    if ($salida -match '(?s)^(.*)\n___HTTP___(\d+)___([\d\.]+)\s*$') {
        $cuerpo = $Matches[1]
        $codigo = $Matches[2]
        $tiempo = $Matches[3]
    }

    return [pscustomobject]@{ Codigo = $codigo; Tiempo = $tiempo; Cuerpo = $cuerpo }
}

# ---------------------------------------------------------------------------
#  Escribe un bloque peticion + respuesta con formato legible.
# ---------------------------------------------------------------------------
function Escribir-Caso {
    param(
        [string]$Titulo,
        [string]$Metodo = 'GET',
        [string]$Url,
        [string[]]$Cabeceras = @(),
        [string]$Cuerpo = $null,
        [string]$Nota = $null,
        [System.Collections.ArrayList]$Lineas
    )

    $r = Invoke-Api -Metodo $Metodo -Url $Url -Cabeceras $Cabeceras -Cuerpo $Cuerpo

    [void]$Lineas.Add('')
    [void]$Lineas.Add("--- $Titulo ---")
    [void]$Lineas.Add('')
    $peticion = "  $Metodo $Url"
    [void]$Lineas.Add($peticion)
    foreach ($c in $Cabeceras) { [void]$Lineas.Add("  $c") }
    if ($Cuerpo) { [void]$Lineas.Add("  body: $Cuerpo") }
    [void]$Lineas.Add('')
    [void]$Lineas.Add("  HTTP $($r.Codigo)   ($($r.Tiempo) s)")
    [void]$Lineas.Add('')
    foreach ($linea in ($r.Cuerpo -split "`n")) { [void]$Lineas.Add("  $linea") }
    if ($Nota) {
        [void]$Lineas.Add('')
        [void]$Lineas.Add("  NOTA: $Nota")
    }
    return $r
}

function Nuevo-Encabezado {
    param([string]$Titulo, [string]$Puerto, [string]$Descripcion)

    $lineas = New-Object System.Collections.ArrayList
    [void]$lineas.Add('===========================================================================')
    [void]$lineas.Add(" $Titulo")
    [void]$lineas.Add(" Puerto $Puerto")
    [void]$lineas.Add(" Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    [void]$lineas.Add('===========================================================================')
    [void]$lineas.Add('')
    [void]$lineas.Add($Descripcion)
    [void]$lineas.Add('')
    [void]$lineas.Add("Swagger UI: http://localhost:$Puerto/swagger-ui.html")

    # Confirma que el servicio responde y se identifica con su propio titulo.
    $doc = Invoke-Api -Url "http://localhost:$Puerto/v3/api-docs"
    $titulo = '(no disponible)'
    if ($doc.Cuerpo -match '"title"\s*:\s*"([^"]+)"') { $titulo = $Matches[1] }
    [void]$lineas.Add("Servicio activo, se identifica como: $titulo")

    # La coma fuerza a devolver la coleccion como un unico objeto. Sin ella
    # PowerShell la desenrolla y la convierte en un array de tamano fijo, al
    # que ya no se le puede agregar nada.
    return ,$lineas
}

function Guardar {
    param([System.Collections.ArrayList]$Lineas, [string]$Archivo)
    $ruta = Join-Path $destino $Archivo
    $Lineas -join "`r`n" | Out-File -FilePath $ruta -Encoding utf8
    Write-Host "  escrito: evidencias\06-apis-consola\$Archivo"
}

Write-Host ''
Write-Host 'Verificando que los cuatro servicios esten arriba...'
$puertos = @{ '8080' = 'banco-core-api'; '8081' = 'bff-web'; '8082' = 'bff-mobile'; '8083' = 'bff-atm' }
$faltantes = @()
foreach ($p in $puertos.Keys) {
    $r = Invoke-Api -Url "http://localhost:$p/v3/api-docs"
    if ($r.Codigo -ne '200') { $faltantes += "$($puertos[$p]) (:$p)" }
}
if ($faltantes.Count -gt 0) {
    Write-Host ''
    Write-Host 'No responden estos servicios:' -ForegroundColor Red
    foreach ($f in $faltantes) { Write-Host "  - $f" -ForegroundColor Red }
    Write-Host ''
    Write-Host 'Levantalos y vuelve a ejecutar este script.'
    exit 1
}
Write-Host '  los cuatro responden.'
Write-Host ''
Write-Host 'Generando evidencia...'


# ===========================================================================
#  1. banco-core-api
# ===========================================================================
$L = Nuevo-Encabezado -Titulo 'API 1 - banco-core-api (servicio de dominio)' -Puerto '8080' `
    -Descripcion @"
Servicio de dominio. Es el unico componente con acceso a PostgreSQL y expone
endpoints genericos sobre las tablas que dejo el batch de la semana 3.
Los tres BFF lo consumen; ningun cliente final lo usa directamente.
"@

[void]$L.Add('')
[void]$L.Add('###########################################################################')
[void]$L.Add(' CONSULTAS')
[void]$L.Add('###########################################################################')

Escribir-Caso -Titulo 'Listado paginado de cuentas' -Url 'http://localhost:8080/api/v1/cuentas?page=0&size=3' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Detalle de una cuenta' -Url 'http://localhost:8080/api/v1/cuentas/101' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Saldo de la cuenta' -Url 'http://localhost:8080/api/v1/cuentas/101/saldo' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Movimientos de la cuenta, filtrados por anio' -Url 'http://localhost:8080/api/v1/cuentas/101/movimientos?anio=2024&size=3' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Estados de cuenta anuales (salida del proceso 3 del batch)' -Url 'http://localhost:8080/api/v1/cuentas/101/estados-anuales' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Intereses calculados (salida del proceso 2 del batch)' -Url 'http://localhost:8080/api/v1/cuentas/101/intereses?size=3' -Lineas $L | Out-Null
Escribir-Caso -Titulo 'Resumen diario de transacciones (salida del proceso 1 del batch)' -Url 'http://localhost:8080/api/v1/transacciones/resumen-diario?size=5' -Lineas $L | Out-Null

[void]$L.Add('')
[void]$L.Add('###########################################################################')
[void]$L.Add(' MANEJO DE ERRORES')
[void]$L.Add('###########################################################################')

Escribir-Caso -Titulo 'Cuenta inexistente' -Url 'http://localhost:8080/api/v1/cuentas/999' `
    -Nota 'Responde 404 con un objeto de error estructurado, no un stacktrace.' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '01-banco-core-api.txt'


# ===========================================================================
#  2. bff-web
# ===========================================================================
$L = Nuevo-Encabezado -Titulo 'API 2 - bff-web (BFF del portal)' -Puerto '8081' `
    -Descripcion @"
Backend dedicado al portal de navegador. Resuelve en paralelo cuatro consultas
al servicio de dominio y devuelve todo lo que la pantalla necesita en una sola
respuesta, para que el navegador no tenga que encadenar llamadas.
"@

Escribir-Caso -Titulo 'Panel completo de la cuenta en una sola llamada' `
    -Url 'http://localhost:8081/bff/web/cuentas/101/panel?tamano=3' `
    -Nota 'El campo meta.milisegundos muestra cuanto tardo el fan-out en paralelo. El campo meta.avisos lo genera el BFF a partir de los datos, no viene del dominio.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Filtrado por anio y paginado' `
    -Url 'http://localhost:8081/bff/web/cuentas/105/panel?anio=2024&pagina=1&tamano=2' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Grilla paginada de cuentas' `
    -Url 'http://localhost:8081/bff/web/cuentas?pagina=0&tamano=3' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Cuenta inexistente' -Url 'http://localhost:8081/bff/web/cuentas/999/panel' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '02-bff-web.txt'


# ===========================================================================
#  3. bff-mobile
# ===========================================================================
$L = Nuevo-Encabezado -Titulo 'API 3 - bff-mobile (BFF de la app movil)' -Puerto '8082' `
    -Descripcion @"
Backend dedicado a la app de telefono. Devuelve lo minimo: saldo y ultimos
cinco movimientos, con nombres de campo cortos, sin campos nulos y con las
descripciones acortadas, para reducir el consumo de datos del usuario.
"@

Escribir-Caso -Titulo 'Pantalla principal de la app' -Url 'http://localhost:8082/bff/movil/cuentas/101' `
    -Nota 'Los movimientos usan f (fecha), t (tipo), m (monto) y d (descripcion). Las claves se repiten en cada item, asi que acortarlas ahorra bytes reales.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Consulta de saldo, la operacion mas frecuente' -Url 'http://localhost:8082/bff/movil/cuentas/101/saldo' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Cuenta inexistente' -Url 'http://localhost:8082/bff/movil/cuentas/999' `
    -Nota 'El error tambien es compacto: solo codigo y mensaje, sin ruta ni marca de tiempo.' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '03-bff-mobile.txt'


# ===========================================================================
#  4. bff-atm
# ===========================================================================
$cab = @("X-ATM-Terminal: $ATM_TERMINAL", "X-ATM-Key: $ATM_KEY")
$referencia = "EVID-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

$L = Nuevo-Encabezado -Titulo 'API 4 - bff-atm (BFF de cajeros automaticos)' -Puerto '8083' `
    -Descripcion @"
Backend dedicado a los cajeros. Es el unico que ejecuta operaciones sobre el
dinero, por lo tanto el unico con autenticacion. Cada terminal se identifica
en cada peticion con su credencial.
"@

[void]$L.Add('')
[void]$L.Add('###########################################################################')
[void]$L.Add(' SEGURIDAD')
[void]$L.Add('###########################################################################')

Escribir-Caso -Titulo 'Sin credencial' -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' `
    -Nota 'Rechazado con 401 antes de llegar al controlador.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Terminal valido con clave incorrecta' -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' `
    -Cabeceras @("X-ATM-Terminal: $ATM_TERMINAL", 'X-ATM-Key: clave-equivocada') -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Terminal inexistente' -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' `
    -Cabeceras @('X-ATM-Terminal: ATM-999', 'X-ATM-Key: loquesea') `
    -Nota 'Mismo mensaje que el caso anterior, a proposito: si se distinguieran, se podrian enumerar los terminales validos probando de a uno.' -Lineas $L | Out-Null

[void]$L.Add('')
[void]$L.Add('###########################################################################')
[void]$L.Add(' OPERACIONES')
[void]$L.Add('###########################################################################')

$antes = Escribir-Caso -Titulo 'Consulta de saldo con credencial valida' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Cabeceras $cab `
    -Nota 'maximoRetirable ya viene calculado por el BFF: el minimo entre el saldo y el tope por operacion, redondeado hacia abajo a la denominacion del cajero.' -Lineas $L

Escribir-Caso -Titulo 'Retiro valido de 10000' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cab `
    -Cuerpo "{`"monto`":10000,`"referencia`":`"$referencia`"}" -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Se repite la MISMA referencia (idempotencia)' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cab `
    -Cuerpo "{`"monto`":10000,`"referencia`":`"$referencia`"}" `
    -Nota 'Devuelve el mismo comprobante con duplicado=true y el mismo saldoResultante. No vuelve a debitar.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Saldo despues de los dos intentos' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Cabeceras $cab `
    -Nota "El saldo bajo una sola vez respecto del inicial, que era $($antes.Cuerpo)" -Lineas $L | Out-Null

[void]$L.Add('')
[void]$L.Add('###########################################################################')
[void]$L.Add(' VALIDACIONES DE NEGOCIO')
[void]$L.Add('###########################################################################')

Escribir-Caso -Titulo 'Monto que no es multiplo de la denominacion' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cab `
    -Cuerpo '{"monto":2500,"referencia":"EVID-DENOM"}' `
    -Nota 'Se rechaza en el BFF, sin llegar al servicio de dominio: un cajero solo entrega billetes de la denominacion que tiene cargada.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Monto sobre el tope por operacion' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cab `
    -Cuerpo '{"monto":300000,"referencia":"EVID-TOPE"}' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Monto negativo' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cab `
    -Cuerpo '{"monto":-5000,"referencia":"EVID-NEG"}' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Saldo insuficiente' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/150/retiro' -Cabeceras $cab `
    -Cuerpo '{"monto":200000,"referencia":"EVID-SALDO"}' `
    -Nota 'Devuelve 422 y no 400: la peticion esta bien formada, es el negocio el que no puede cumplirla. El cajero usa esa diferencia para ofrecer el maximo disponible en vez de pedir otro monto a ciegas.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Cuenta inexistente' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/999/retiro' -Cabeceras $cab `
    -Cuerpo '{"monto":5000,"referencia":"EVID-NOCTA"}' -Lineas $L | Out-Null

[void]$L.Add('')
[void]$L.Add('Codigos HTTP usados y por que:')
[void]$L.Add('  400  peticion mal formada o monto que el cajero no puede entregar')
[void]$L.Add('  401  terminal no autorizado')
[void]$L.Add('  404  la cuenta no existe')
[void]$L.Add('  409  no se pudo confirmar el retiro: NO entregar efectivo, conciliar')
[void]$L.Add('  422  peticion correcta que el negocio no puede cumplir')
[void]$L.Add('  503  el servicio de dominio no responde')

Guardar -Lineas $L -Archivo '04-bff-atm.txt'


# ===========================================================================
#  5. Comparativa entre los tres BFF
# ===========================================================================
$L = New-Object System.Collections.ArrayList
[void]$L.Add('===========================================================================')
[void]$L.Add(' COMPARATIVA - LA MISMA CUENTA VISTA POR LOS TRES BFF')
[void]$L.Add(" Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
[void]$L.Add('===========================================================================')
[void]$L.Add('')
[void]$L.Add('Los tres devuelven la cuenta 101, pero cada uno decide que incluir.')

$web = Invoke-Api -Url 'http://localhost:8081/bff/web/cuentas/101/panel'
$movil = Invoke-Api -Url 'http://localhost:8082/bff/movil/cuentas/101'
$movilSaldo = Invoke-Api -Url 'http://localhost:8082/bff/movil/cuentas/101/saldo'
$atm = Invoke-Api -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Cabeceras $cab

$bytesWeb = [System.Text.Encoding]::UTF8.GetByteCount($web.Cuerpo)
$bytesMovil = [System.Text.Encoding]::UTF8.GetByteCount($movil.Cuerpo)
$bytesMovilSaldo = [System.Text.Encoding]::UTF8.GetByteCount($movilSaldo.Cuerpo)
$bytesAtm = [System.Text.Encoding]::UTF8.GetByteCount($atm.Cuerpo)

[void]$L.Add('')
[void]$L.Add('--- Tamano de la respuesta ---')
[void]$L.Add('')
[void]$L.Add(('  {0,-44} {1,8}' -f 'RESPUESTA', 'BYTES'))
[void]$L.Add('  ' + ('-' * 53))
[void]$L.Add(('  {0,-44} {1,8}' -f 'bff-web   panel completo', $bytesWeb))
[void]$L.Add(('  {0,-44} {1,8}' -f 'bff-movil resumen', $bytesMovil))
[void]$L.Add(('  {0,-44} {1,8}' -f 'bff-atm   saldo', $bytesAtm))
[void]$L.Add(('  {0,-44} {1,8}' -f 'bff-movil solo saldo', $bytesMovilSaldo))
[void]$L.Add('')
$reduccion = [math]::Round(100 - ($bytesMovil * 100 / $bytesWeb))
[void]$L.Add("  El BFF movil transmite un $reduccion% menos que el BFF web para la misma cuenta.")

[void]$L.Add('')
[void]$L.Add('--- Respuesta del BFF web ---')
[void]$L.Add('')
foreach ($linea in ($web.Cuerpo -split "`n")) { [void]$L.Add("  $linea") }
[void]$L.Add('')
[void]$L.Add('--- Respuesta del BFF movil ---')
[void]$L.Add('')
foreach ($linea in ($movil.Cuerpo -split "`n")) { [void]$L.Add("  $linea") }
[void]$L.Add('')
[void]$L.Add('--- Respuesta del BFF de cajeros ---')
[void]$L.Add('')
foreach ($linea in ($atm.Cuerpo -split "`n")) { [void]$L.Add("  $linea") }

Guardar -Lineas $L -Archivo '05-comparativa-bff.txt'

Write-Host ''
Write-Host 'Listo. Evidencia en evidencias\06-apis-consola\' -ForegroundColor Green
Write-Host ''
