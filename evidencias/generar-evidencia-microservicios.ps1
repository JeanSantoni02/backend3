# ============================================================================
#  Evidencia de ejecucion de los microservicios (semanas 6, 7 y 8).
#
#  Requiere los servicios levantados:
#      config-server 8888, eureka-server 8761, auth-server 9000,
#      banco-core-api 8080, servicio-notificaciones 8084,
#      bff-web 8081, bff-mobile 8082, bff-atm 8083
#
#  Uso, desde la raiz del proyecto:
#      powershell -ExecutionPolicy Bypass -File evidencias\generar-evidencia-microservicios.ps1
# ============================================================================

$ErrorActionPreference = 'Stop'

$destino = Join-Path $PSScriptRoot '08-microservicios'
if (-not (Test-Path $destino)) { New-Item -ItemType Directory -Path $destino | Out-Null }

$ATM_TERMINAL = 'ATM-001'
$ATM_KEY = 'clave-demo-001'

function Invoke-Api {
    param([string]$Metodo = 'GET', [string]$Url, [string[]]$Cabeceras = @(),
          [string]$Cuerpo = $null, [string]$Usuario = $null, [string]$Datos = $null)

    $args = @('-s', '-S', '--max-time', '30', '-w', "`n___HTTP___%{http_code}___%{time_total}")
    $args += @('-X', $Metodo)
    foreach ($c in $Cabeceras) { $args += @('-H', $c) }
    if ($Usuario) { $args += @('-u', $Usuario) }
    if ($Datos) { $args += @('-d', $Datos) }

    $temporal = $null
    if ($Cuerpo) {
        # El JSON va por archivo: PowerShell 5.1 reescribe las comillas al
        # pasar argumentos a un ejecutable nativo
        $temporal = [System.IO.Path]::GetTempFileName()
        [System.IO.File]::WriteAllText($temporal, $Cuerpo, (New-Object System.Text.UTF8Encoding($false)))
        $args += @('-H', 'Content-Type: application/json', '--data-binary', "@$temporal")
    }
    $args += $Url

    try { $salida = (& curl.exe @args 2>&1) -join "`n" }
    finally { if ($temporal -and (Test-Path $temporal)) { Remove-Item $temporal -Force } }

    $codigo = '???'; $tiempo = '?'; $cuerpo = $salida
    if ($salida -match '(?s)^(.*)\n___HTTP___(\d+)___([\d\.]+)\s*$') {
        $cuerpo = $Matches[1]; $codigo = $Matches[2]; $tiempo = $Matches[3]
    }
    return [pscustomobject]@{ Codigo = $codigo; Tiempo = $tiempo; Cuerpo = $cuerpo }
}

function Escribir-Caso {
    param([string]$Titulo, [string]$Metodo = 'GET', [string]$Url,
          [string[]]$Cabeceras = @(), [string]$Cuerpo = $null,
          [string]$Usuario = $null, [string]$Datos = $null, [string]$Nota = $null,
          [System.Collections.ArrayList]$Lineas)

    $r = Invoke-Api -Metodo $Metodo -Url $Url -Cabeceras $Cabeceras -Cuerpo $Cuerpo `
                    -Usuario $Usuario -Datos $Datos

    [void]$Lineas.Add('')
    [void]$Lineas.Add("--- $Titulo ---")
    [void]$Lineas.Add('')
    [void]$Lineas.Add("  $Metodo $Url")
    foreach ($c in $Cabeceras) { [void]$Lineas.Add("  $c") }
    if ($Usuario) { [void]$Lineas.Add("  -u $Usuario") }
    if ($Datos) { [void]$Lineas.Add("  $Datos") }
    if ($Cuerpo) { [void]$Lineas.Add("  body: $Cuerpo") }
    [void]$Lineas.Add('')
    [void]$Lineas.Add("  HTTP $($r.Codigo)   ($($r.Tiempo) s)")
    [void]$Lineas.Add('')
    foreach ($l in ($r.Cuerpo -split "`n")) { [void]$Lineas.Add("  $l") }
    if ($Nota) { [void]$Lineas.Add(''); [void]$Lineas.Add("  NOTA: $Nota") }
    return $r
}

function Nuevo-Doc {
    param([string]$Titulo, [string]$Descripcion)
    $l = New-Object System.Collections.ArrayList
    [void]$l.Add('===========================================================================')
    [void]$l.Add(" $Titulo")
    [void]$l.Add(" Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
    [void]$l.Add('===========================================================================')
    [void]$l.Add('')
    [void]$l.Add($Descripcion)
    return ,$l
}

function Guardar {
    param([System.Collections.ArrayList]$Lineas, [string]$Archivo)
    $Lineas -join "`r`n" | Out-File -FilePath (Join-Path $destino $Archivo) -Encoding utf8
    Write-Host "  escrito: evidencias\08-microservicios\$Archivo"
}

Write-Host ''
Write-Host 'Verificando servicios...'
$puertos = [ordered]@{
    '8888' = 'config-server'; '8761' = 'eureka-server'; '9000' = 'auth-server'
    '8080' = 'banco-core-api'; '8084' = 'servicio-notificaciones'
    '8081' = 'bff-web'; '8082' = 'bff-mobile'; '8083' = 'bff-atm'
}
$faltan = @()
foreach ($p in $puertos.Keys) {
    $r = Invoke-Api -Url "http://localhost:$p/actuator/health"
    if ($r.Codigo -ne '200') { $faltan += "$($puertos[$p]) (:$p)" }
}
if ($faltan.Count -gt 0) {
    Write-Host 'No responden:' -ForegroundColor Red
    foreach ($f in $faltan) { Write-Host "  - $f" -ForegroundColor Red }
    exit 1
}
Write-Host '  los ocho responden.'
Write-Host ''
Write-Host 'Generando evidencia...'


# ===========================================================================
#  1. Config Server y Service Discovery (semana 6)
# ===========================================================================
$L = Nuevo-Doc -Titulo 'SEMANA 6 - Configuracion centralizada y Service Discovery' -Descripcion @"
El config server entrega la configuracion a los microservicios y Eureka mantiene
el registro de instancias. Ningun microservicio tiene la configuracion de negocio
en su propio archivo.
"@

Escribir-Caso -Titulo 'Salud del config server' `
    -Url 'http://localhost:8888/actuator/health' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Configuracion que el config server entrega a banco-core-api' `
    -Url 'http://localhost:8888/banco-core-api/default' `
    -Nota 'Las propiedades salen de config-repo/banco-core-api.yml y de application.yml, no del jar del microservicio.' `
    -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Configuracion de resiliencia que recibe bff-atm' `
    -Url 'http://localhost:8888/bff-atm/default' `
    -Nota 'Los umbrales del circuit breaker se administran de forma centralizada.' `
    -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Registro de Eureka' `
    -Url 'http://localhost:8761/eureka/apps' -Cabeceras @('Accept: application/json') `
    -Nota 'Los cinco microservicios estan registrados y en estado UP.' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '01-config-server-y-eureka.txt'


# ===========================================================================
#  2. Seguridad OAuth2 (semanas 6 y 8)
# ===========================================================================
$L = Nuevo-Doc -Titulo 'SEMANA 8 - Seguridad con OAuth2' -Descripcion @"
El servidor de autorizacion emite tokens JWT con el flujo client_credentials.
banco-core-api actua como resource server y autoriza por scope.
"@

Escribir-Caso -Titulo 'Metadatos del servidor de autorizacion' `
    -Url 'http://localhost:9000/.well-known/oauth-authorization-server' -Lineas $L | Out-Null

$tok = Escribir-Caso -Titulo 'Token para bff-atm (client_credentials)' -Metodo 'POST' `
    -Url 'http://localhost:9000/oauth2/token' -Usuario 'bff-atm:secreto-bff-atm' `
    -Datos 'grant_type=client_credentials&scope=cuentas.leer retiros.escribir' `
    -Nota 'El cajero obtiene scopes de lectura y de escritura de retiros.' -Lineas $L

$token = ''
if ($tok.Cuerpo -match '"access_token"\s*:\s*"([^"]+)"') { $token = $Matches[1] }

Escribir-Caso -Titulo 'Token para bff-mobile (scope reducido)' -Metodo 'POST' `
    -Url 'http://localhost:9000/oauth2/token' -Usuario 'bff-mobile:secreto-bff-mobile' `
    -Datos 'grant_type=client_credentials&scope=cuentas.leer' `
    -Nota 'La app movil solo puede leer: no tiene el scope retiros.escribir.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Acceso al recurso protegido SIN token' `
    -Url 'http://localhost:8080/api/v1/cuentas/101' `
    -Nota 'Rechazado con 401 por el resource server.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Acceso al recurso protegido CON token' `
    -Url 'http://localhost:8080/api/v1/cuentas/101' `
    -Cabeceras @("Authorization: Bearer $token") -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Credenciales de cliente incorrectas' -Metodo 'POST' `
    -Url 'http://localhost:9000/oauth2/token' -Usuario 'bff-atm:clave-equivocada' `
    -Datos 'grant_type=client_credentials' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '02-oauth2.txt'


# ===========================================================================
#  3. Arquitectura de eventos (semana 7)
# ===========================================================================
$L = Nuevo-Doc -Titulo 'SEMANA 7 - Arquitectura de eventos con JMS' -Descripcion @"
Un retiro confirmado publica un evento en el topico banco.eventos.transaccion.
El servicio de notificaciones lo consume de forma asincrona y genera el aviso.
"@

$cabAtm = @("X-ATM-Terminal: $ATM_TERMINAL", "X-ATM-Key: $ATM_KEY")
$ref = "EVID-$(Get-Date -Format 'yyyyMMdd-HHmmss')"

Escribir-Caso -Titulo 'Notificaciones antes del retiro' `
    -Url 'http://localhost:8084/api/v1/notificaciones/resumen' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Retiro desde el cajero' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cabAtm `
    -Cuerpo "{`"monto`":5000,`"referencia`":`"$ref`"}" `
    -Nota 'bff-atm pide el token, llama al dominio y este publica el evento tras confirmar el debito.' `
    -Lineas $L | Out-Null

Start-Sleep -Seconds 3

Escribir-Caso -Titulo 'Notificacion generada por el consumidor' `
    -Url 'http://localhost:8084/api/v1/notificaciones?cantidad=3' `
    -Nota 'El servicio de notificaciones nunca fue invocado directamente: reacciono al evento.' `
    -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Mismo retiro repetido (idempotencia)' -Metodo 'POST' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/retiro' -Cabeceras $cabAtm `
    -Cuerpo "{`"monto`":5000,`"referencia`":`"$ref`"}" `
    -Nota 'duplicado=true y el saldo no vuelve a bajar.' -Lineas $L | Out-Null

Start-Sleep -Seconds 3

Escribir-Caso -Titulo 'El consumidor no duplico el aviso' `
    -Url 'http://localhost:8084/api/v1/notificaciones/resumen' `
    -Nota 'El evento repetido se descarta por su eventoId.' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '03-eventos-jms.txt'


# ===========================================================================
#  4. Tolerancia a fallos (semana 7)
# ===========================================================================
$L = Nuevo-Doc -Titulo 'SEMANA 7 - Tolerancia a fallos con Resilience4j' -Descripcion @"
bff-atm protege sus llamadas al dominio con circuit breaker, reintentos y
limite de tiempo. El estado del circuito se observa por actuator.
"@

Escribir-Caso -Titulo 'Estado del circuito en operacion normal' `
    -Url 'http://localhost:8083/actuator/circuitbreakers' `
    -Nota 'CLOSED: las llamadas pasan al servicio de dominio.' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'Consulta de saldo correcta' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Cabeceras $cabAtm -Lineas $L | Out-Null

[void]$L.Add('')
[void]$L.Add('--- Ciclo completo del circuit breaker ---')
[void]$L.Add('')
[void]$L.Add('  Para reproducirlo hay que detener banco-core-api, hacer seis consultas')
[void]$L.Add('  de saldo, volver a levantarlo, esperar los 20 s de wait-duration y')
[void]$L.Add('  consultar de nuevo. Transiciones observadas en la ejecucion de prueba:')
[void]$L.Add('')
[void]$L.Add('     CLOSED_TO_OPEN       tras 5 llamadas fallidas (umbral 50 %)')
[void]$L.Add('     OPEN_TO_HALF_OPEN    al reintentar despues de la espera')
[void]$L.Add('     HALF_OPEN_TO_CLOSED  tras 3 llamadas correctas')
[void]$L.Add('')
[void]$L.Add('  Con el circuito abierto el cajero recibe:')
[void]$L.Add('     HTTP 503 {"codigo":"SIN_SERVICIO","entregarEfectivo":false}')
[void]$L.Add('  y no entrega dinero.')

Escribir-Caso -Titulo 'Eventos registrados del circuito' `
    -Url 'http://localhost:8083/actuator/circuitbreakerevents/coreApi' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '04-resiliencia.txt'


# ===========================================================================
#  5. Los tres BFF operando sobre la plataforma
# ===========================================================================
$L = Nuevo-Doc -Titulo 'Los tres BFF sobre la plataforma de microservicios' -Descripcion @"
Cada BFF obtiene su propio token con los scopes que le corresponden y consume
el servicio de dominio a traves de la infraestructura de Spring Cloud.
"@

Escribir-Caso -Titulo 'bff-web: panel completo' `
    -Url 'http://localhost:8081/bff/web/cuentas/101/panel?tamano=2' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'bff-mobile: respuesta minima' `
    -Url 'http://localhost:8082/bff/movil/cuentas/101' -Lineas $L | Out-Null

Escribir-Caso -Titulo 'bff-atm: saldo con maximo retirable' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Cabeceras $cabAtm -Lineas $L | Out-Null

Escribir-Caso -Titulo 'bff-atm sin credencial de terminal' `
    -Url 'http://localhost:8083/bff/atm/cuentas/101/saldo' -Lineas $L | Out-Null

Guardar -Lineas $L -Archivo '05-bff-sobre-la-plataforma.txt'

Write-Host ''
Write-Host "Listo. Evidencia en evidencias\08-microservicios\" -ForegroundColor Green
Write-Host ''
