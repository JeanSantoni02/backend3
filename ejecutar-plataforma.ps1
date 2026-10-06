# Levanta los servicios en el orden que exigen sus dependencias

$raiz = $PSScriptRoot
$logs = Join-Path $raiz "logs"
if (-not (Test-Path $logs)) { New-Item -ItemType Directory -Path $logs -Force | Out-Null }

if (-not $env:DB_PASSWORD) {
    $segura = Read-Host "Contrasena de postgres" -AsSecureString
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
}

# Orden de dependencias; servicio-pagos levanta dos instancias para el balanceo
$servicios = @(
    @{ modulo = "config-server";           puerto = 8888 },
    @{ modulo = "eureka-server";           puerto = 8761 },
    @{ modulo = "auth-server";             puerto = 9000 },
    @{ modulo = "banco-core-api";          puerto = 8080 },
    @{ modulo = "servicio-notificaciones"; puerto = 8084 },
    @{ modulo = "servicio-clientes";       puerto = 8085 },
    @{ modulo = "servicio-cuentas";        puerto = 8086 },
    @{ modulo = "servicio-pagos";          puerto = 8087 },
    @{ modulo = "servicio-pagos";          puerto = 8088 },
    @{ modulo = "bff-web";                 puerto = 8081 },
    @{ modulo = "bff-mobile";              puerto = 8082 },
    @{ modulo = "bff-atm";                 puerto = 8083 },
    @{ modulo = "api-gateway";             puerto = 8443; https = $true }
)

function Esta-Ocupado($puerto) {
    $null -ne (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}

function Esperar-Arranque($s, $segundos = 120) {
    $esquema = if ($s.https) { "https" } else { "http" }
    $url = "${esquema}://localhost:$($s.puerto)/actuator/health"
    $fin = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $fin) {
        # -k acepta el certificado de desarrollo del gateway
        $codigo = curl.exe -sk -o NUL -w "%{http_code}" $url 2>$null
        if ($codigo -eq "200") { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

foreach ($s in $servicios) {
    $jar = Join-Path $raiz "$($s.modulo)\target\$($s.modulo)-1.0-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "Falta $jar" -ForegroundColor Red
        Write-Host "Compila primero con: mvn clean package" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
foreach ($s in $servicios) {
    $etiqueta = "$($s.modulo):$($s.puerto)"
    if (Esta-Ocupado $s.puerto) {
        Write-Host ("  {0,-30} ya estaba arriba" -f $etiqueta) -ForegroundColor DarkGray
        continue
    }

    $jar = Join-Path $raiz "$($s.modulo)\target\$($s.modulo)-1.0-SNAPSHOT.jar"
    $log = Join-Path $logs "$($s.modulo)-$($s.puerto).log"
    $env:PORT = "$($s.puerto)"

    Start-Process -FilePath "java" `
        -ArgumentList "-Xms64m", "-Xmx320m", "-jar", $jar `
        -WorkingDirectory $raiz `
        -RedirectStandardOutput $log `
        -RedirectStandardError (Join-Path $logs "$($s.modulo)-$($s.puerto).err.log") `
        -WindowStyle Hidden | Out-Null

    Write-Host ("  {0,-30} arrancando..." -f $etiqueta) -NoNewline
    if (Esperar-Arranque $s) {
        Write-Host " listo" -ForegroundColor Green
    } else {
        Write-Host " no respondio" -ForegroundColor Red
        Write-Host "    revisa $log" -ForegroundColor Yellow
        exit 1
    }
}
Remove-Item Env:\PORT -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Plataforma arriba." -ForegroundColor Green
Write-Host ""
Write-Host "  Entrada unica (HTTPS)  https://localhost:8443"
Write-Host "  Eureka                 http://localhost:8761"
Write-Host "  Clientes               http://localhost:8085/swagger-ui.html"
Write-Host "  Cuentas                http://localhost:8086/swagger-ui.html"
Write-Host "  Pagos                  http://localhost:8087/swagger-ui.html"
Write-Host ""
Write-Host "Logs en la carpeta logs\. Para detener todo: detener-plataforma.ps1"
Write-Host ""
