# Levanta los ocho servicios en el orden que exigen sus dependencias.
# Uso:  powershell -ExecutionPolicy Bypass -File ejecutar-plataforma.ps1

$raiz = $PSScriptRoot
$logs = Join-Path $raiz "logs"
if (-not (Test-Path $logs)) { New-Item -ItemType Directory -Path $logs -Force | Out-Null }

if (-not $env:DB_PASSWORD) {
    $segura = Read-Host "Contrasena de postgres" -AsSecureString
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
}

# El orden importa: cada servicio necesita que el anterior ya este arriba.
$servicios = @(
    @{ nombre = "config-server";           puerto = 8888 },
    @{ nombre = "eureka-server";           puerto = 8761 },
    @{ nombre = "auth-server";             puerto = 9000 },
    @{ nombre = "banco-core-api";          puerto = 8080 },
    @{ nombre = "servicio-notificaciones"; puerto = 8084 },
    @{ nombre = "bff-web";                 puerto = 8081 },
    @{ nombre = "bff-mobile";              puerto = 8082 },
    @{ nombre = "bff-atm";                 puerto = 8083 }
)

function Esta-Ocupado($puerto) {
    $null -ne (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue)
}

function Esperar-Arranque($nombre, $puerto, $segundos = 90) {
    $fin = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $fin) {
        try {
            $r = Invoke-WebRequest "http://localhost:$puerto/actuator/health" -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 2
    }
    return $false
}

$jars = @{}
foreach ($s in $servicios) {
    $jar = Join-Path $raiz "$($s.nombre)\target\$($s.nombre)-1.0-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        Write-Host "Falta $jar" -ForegroundColor Red
        Write-Host "Compila primero con: mvn clean package" -ForegroundColor Yellow
        exit 1
    }
    $jars[$s.nombre] = $jar
}

Write-Host ""
foreach ($s in $servicios) {
    if (Esta-Ocupado $s.puerto) {
        Write-Host ("  {0,-24} ya estaba arriba en :{1}" -f $s.nombre, $s.puerto) -ForegroundColor DarkGray
        continue
    }

    $log = Join-Path $logs "$($s.nombre).log"
    Start-Process -FilePath "java" `
        -ArgumentList "-jar", $jars[$s.nombre] `
        -WorkingDirectory $raiz `
        -RedirectStandardOutput $log `
        -RedirectStandardError (Join-Path $logs "$($s.nombre).err.log") `
        -WindowStyle Hidden | Out-Null

    Write-Host ("  {0,-24} arrancando en :{1} ..." -f $s.nombre, $s.puerto) -NoNewline
    if (Esperar-Arranque $s.nombre $s.puerto) {
        Write-Host " listo" -ForegroundColor Green
    } else {
        Write-Host " no respondio" -ForegroundColor Red
        Write-Host "    revisa $log" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""
Write-Host "Plataforma arriba." -ForegroundColor Green
Write-Host ""
Write-Host "  Eureka        http://localhost:8761"
Write-Host "  Dominio       http://localhost:8080/swagger-ui.html"
Write-Host "  BFF Web       http://localhost:8081/swagger-ui.html"
Write-Host "  BFF Movil     http://localhost:8082/swagger-ui.html"
Write-Host "  BFF Cajeros   http://localhost:8083/swagger-ui.html"
Write-Host "  Notificaciones http://localhost:8084/swagger-ui.html"
Write-Host ""
Write-Host "Logs en la carpeta logs\. Para detener todo: detener-plataforma.ps1"
Write-Host ""
