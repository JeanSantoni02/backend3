# ============================================================================
#  Genera capturas de pantalla de las cuatro APIs usando Chrome en modo
#  headless. No abre ninguna ventana ni toca lo que tengas en pantalla.
#
#  Requisitos: Google Chrome instalado y los cuatro servicios corriendo.
#
#  Uso, parado en la raiz del proyecto:
#      powershell -ExecutionPolicy Bypass -File evidencias\generar-capturas.ps1
#
#  Deja los PNG en evidencias\07-capturas\
# ============================================================================

$ErrorActionPreference = 'Stop'

$destino = Join-Path $PSScriptRoot '07-capturas'
if (-not (Test-Path $destino)) { New-Item -ItemType Directory -Path $destino | Out-Null }

# Perfil temporal aparte para no tocar tu perfil real de Chrome.
$perfil = Join-Path $env:TEMP 'chrome-capturas-banco'

$rutasChrome = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
)
$navegador = $rutasChrome | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $navegador) {
    Write-Host 'No se encontro Chrome ni Edge.' -ForegroundColor Red
    exit 1
}
Write-Host ''
Write-Host "Navegador: $navegador"

function Capturar {
    param(
        [string]$Url,
        [string]$Archivo,
        [int]$Ancho = 1500,
        [int]$Alto = 1100,
        [string]$Descripcion
    )

    $ruta = Join-Path $destino $Archivo
    $argumentos = @(
        '--headless=new',
        '--disable-gpu',
        '--no-first-run',
        '--hide-scrollbars',
        '--force-light-mode',
        '--blink-settings=preferredColorScheme=1',
        "--user-data-dir=$perfil",
        "--window-size=$Ancho,$Alto",
        '--virtual-time-budget=20000',
        "--screenshot=$ruta",
        $Url
    )

    # Start-Process y no llamada directa: Chrome escribe en stderr el mensaje de
    # confirmacion, y en PowerShell 5.1 redirigir stderr de un ejecutable nativo
    # con 2>&1 lo convierte en un error de terminacion aunque todo haya salido bien.
    $salidaTmp = [System.IO.Path]::GetTempFileName()
    $errorTmp = [System.IO.Path]::GetTempFileName()
    $entrecomillados = $argumentos | ForEach-Object {
        if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
    }
    Start-Process -FilePath $navegador -ArgumentList $entrecomillados -Wait -NoNewWindow `
        -RedirectStandardOutput $salidaTmp -RedirectStandardError $errorTmp | Out-Null
    Remove-Item $salidaTmp, $errorTmp -Force -ErrorAction SilentlyContinue

    if (Test-Path $ruta) {
        $kb = [math]::Round((Get-Item $ruta).Length / 1KB)
        Write-Host ("  {0,-42} {1,5} KB   {2}" -f $Archivo, $kb, $Descripcion)
    } else {
        Write-Host "  FALLO: $Archivo" -ForegroundColor Red
    }
}

Write-Host ''
Write-Host 'Verificando servicios...'
$puertos = @('8080', '8081', '8082', '8083')
foreach ($p in $puertos) {
    $r = & curl.exe -s -o NUL -w '%{http_code}' --max-time 5 "http://localhost:$p/v3/api-docs"
    if ($r -ne '200') {
        Write-Host "  El servicio en :$p no responde. Levantalo y reintenta." -ForegroundColor Red
        exit 1
    }
}
Write-Host '  los cuatro responden.'

Write-Host ''
Write-Host 'Capturando documentacion interactiva (Swagger UI)...'
Capturar -Url 'http://localhost:8080/swagger-ui.html' -Archivo '01-swagger-banco-core-api.png' -Alto 1200 -Descripcion 'servicio de dominio'
Capturar -Url 'http://localhost:8081/swagger-ui.html' -Archivo '02-swagger-bff-web.png' -Descripcion 'BFF web'
Capturar -Url 'http://localhost:8082/swagger-ui.html' -Archivo '03-swagger-bff-mobile.png' -Descripcion 'BFF movil'
Capturar -Url 'http://localhost:8083/swagger-ui.html' -Archivo '04-swagger-bff-atm.png' -Descripcion 'BFF cajeros'

Write-Host ''
Write-Host 'Capturando respuestas de las APIs...'
Capturar -Url 'http://localhost:8080/api/v1/cuentas/101' -Archivo '05-core-api-cuenta.png' -Alto 260 -Descripcion 'detalle de cuenta'
Capturar -Url 'http://localhost:8080/api/v1/cuentas/101/estados-anuales' -Archivo '06-core-api-estado-anual.png' -Alto 260 -Descripcion 'salida del proceso 3 del batch'
Capturar -Url 'http://localhost:8080/api/v1/transacciones/resumen-diario?size=5' -Archivo '07-core-api-resumen-diario.png' -Alto 420 -Descripcion 'salida del proceso 1 del batch'
Capturar -Url 'http://localhost:8081/bff/web/cuentas/101/panel?tamano=3' -Archivo '08-bff-web-panel.png' -Alto 320 -Descripcion 'respuesta completa del portal'
Capturar -Url 'http://localhost:8082/bff/movil/cuentas/101' -Archivo '09-bff-movil-resumen.png' -Alto 220 -Descripcion 'respuesta minima de la app'
Capturar -Url 'http://localhost:8082/bff/movil/cuentas/101/saldo' -Archivo '10-bff-movil-saldo.png' -Alto 170 -Descripcion 'solo el saldo'
Capturar -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Archivo '11-bff-atm-sin-credencial.png' -Alto 170 -Descripcion 'rechazo 401 sin credencial'
Capturar -Url 'http://localhost:8080/api/v1/cuentas/999' -Archivo '12-core-api-error-404.png' -Alto 220 -Descripcion 'manejo de error'

Write-Host ''
Write-Host "Listo. Capturas en evidencias\07-capturas\" -ForegroundColor Green
Write-Host ''
Write-Host 'Nota: la captura 11 muestra el rechazo por falta de credencial, que es'
Write-Host 'el comportamiento correcto. Para capturar los endpoints autenticados del'
Write-Host 'cajero usa Swagger UI (boton Try it out) o Postman, agregando las'
Write-Host 'cabeceras X-ATM-Terminal y X-ATM-Key.'
Write-Host ''
