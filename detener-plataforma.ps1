# Detiene los ocho servicios liberando sus puertos.
# Uso:  powershell -ExecutionPolicy Bypass -File detener-plataforma.ps1

$puertos = @(8083, 8082, 8081, 8084, 8080, 9000, 8761, 8888)

Write-Host ""
foreach ($puerto in $puertos) {
    $conexion = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
    if (-not $conexion) {
        Write-Host ("  :{0,-6} ya estaba libre" -f $puerto) -ForegroundColor DarkGray
        continue
    }
    $conexion | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object {
        Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
        Write-Host ("  :{0,-6} detenido" -f $puerto) -ForegroundColor Green
    }
}
Write-Host ""
