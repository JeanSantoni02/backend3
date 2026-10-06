# Recorre el sistema por el gateway y registra cada respuesta; requiere la plataforma arriba

$G = "https://localhost:8443"
$AUTH = "http://localhost:9000/oauth2/token"
$raiz = Split-Path $PSScriptRoot -Parent
$salida = Join-Path $PSScriptRoot "09-eft\02-microservicios.txt"
$tmp = Join-Path $env:TEMP "eft-cuerpo.json"
$marca = Get-Date -Format "HHmmss"

if (-not $env:DB_PASSWORD) {
    $segura = Read-Host "Contrasena de postgres" -AsSecureString
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura))
}

$l = New-Object System.Collections.ArrayList
function A($t) { [void]$l.Add($t); Write-Host $t }
function Titulo($t) { A ""; A ("=" * 70); A " $t"; A ("=" * 70) }
function Sub($t) { A ""; A "--- $t" }

function Token($cliente, $secreto, $scopes) {
    $r = curl.exe -s -u "${cliente}:${secreto}" -d "grant_type=client_credentials&scope=$scopes" $AUTH
    return ($r | ConvertFrom-Json).access_token
}

# Devuelve @{ codigo; cuerpo; cabeceras }
function Llamar($metodo, $ruta, $cuerpo, $token, [switch]$cabeceras) {
    $argumentos = @("-sk", "-X", $metodo, "$G$ruta", "-w", "`n%{http_code}")
    if ($token) { $argumentos += @("-H", "Authorization: Bearer $token") }
    if ($cabeceras) { $argumentos += "-i" }
    if ($cuerpo) {
        Set-Content -Path $tmp -Value $cuerpo -Encoding ASCII -NoNewline
        $argumentos += @("-H", "Content-Type: application/json", "--data-binary", "@$tmp")
    }
    $lineas = @(& curl.exe @argumentos)
    $codigo = $lineas[-1]
    $resto = if ($lineas.Count -gt 1) { $lineas[0..($lineas.Count - 2)] -join "`n" } else { "" }
    return @{ codigo = $codigo; cuerpo = $resto }
}

function Mostrar($metodo, $ruta, $cuerpo, $token) {
    A "> $metodo $ruta"
    if ($cuerpo) { A "  $cuerpo" }
    $r = Llamar $metodo $ruta $cuerpo $token
    A ("  HTTP " + $r.codigo)
    if ($r.cuerpo) { A ("  " + $r.cuerpo) }
    return $r
}

function DV($cuerpo) {
    $suma = 0; $factor = 2
    for ($i = $cuerpo.Length - 1; $i -ge 0; $i--) {
        $suma += [int]::Parse($cuerpo[$i]) * $factor
        $factor = if ($factor -eq 7) { 2 } else { $factor + 1 }
    }
    $resto = 11 - ($suma % 11)
    if ($resto -eq 11) { return "0" } elseif ($resto -eq 10) { return "K" } else { return "$resto" }
}

A ("=" * 70)
A " BANCO XYZ - EVIDENCIA DE LA PLATAFORMA DE MICROSERVICIOS"
A " Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
A " Todas las llamadas pasan por el gateway: $G"
A ("=" * 70)

# ---------------------------------------------------------------------------
Titulo "1. SERVICE DISCOVERY"
A "> GET http://localhost:8761/eureka/apps"
$e = Invoke-RestMethod "http://localhost:8761/eureka/apps" -Headers @{ Accept = "application/json" }
foreach ($app in ($e.applications.application | Sort-Object name)) {
    foreach ($inst in @($app.instance)) {
        A ("  {0,-26} {1,-4} {2}" -f $app.name, $inst.status, $inst.instanceId)
    }
}
A ""
A "servicio-pagos tiene dos instancias: el gateway reparte la carga entre ambas."

# ---------------------------------------------------------------------------
Titulo "2. HTTPS EN EL PUNTO DE ENTRADA"
A "> Conexion TLS a localhost:8443"
$tcp = New-Object System.Net.Sockets.TcpClient("localhost", 8443)
# El certificado de desarrollo es autofirmado: se acepta para poder inspeccionarlo
$ssl = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false, { $true })
$ssl.AuthenticateAsClient("localhost")
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($ssl.RemoteCertificate)
A ("  protocolo negociado : " + $ssl.SslProtocol)
A ("  cifrado             : " + $ssl.CipherAlgorithm + " " + $ssl.CipherStrength + " bits")
A ("  certificado         : " + $cert.Subject)
A ("  algoritmo de firma  : " + $cert.SignatureAlgorithm.FriendlyName)
A ("  valido hasta        : " + $cert.NotAfter.ToString("yyyy-MM-dd"))
$san = $cert.Extensions | Where-Object { $_.Oid.FriendlyName -match "Subject Alternative" }
if ($san) { A ("  nombres alternativos: " + $san.Format($false)) }
$ssl.Dispose(); $tcp.Dispose()
A ""
A "> GET http://localhost:8443/actuator/health   (sin TLS)"
$sinTls = curl.exe -s -o NUL -w "%{http_code}" "http://localhost:8443/actuator/health" 2>$null
A "  sin respuesta HTTP (codigo $sinTls): el puerto solo habla TLS"
A ""
A "En la nube el certificado lo emite AWS Certificate Manager y el TLS termina"
A "en el balanceador; el certificado autofirmado es solo para desarrollo."

# ---------------------------------------------------------------------------
Titulo "3. OAUTH2 Y AUTORIZACION POR SCOPE"
$tok = Token "portal-operaciones" "secreto-portal-operaciones" "clientes.leer clientes.escribir cuentas.leer cuentas.escribir pagos.leer pagos.escribir"
$tokMovil = Token "bff-mobile" "secreto-bff-mobile" "cuentas.leer"
A "Token del portal de operaciones: emitido (client_credentials, JWT firmado con RSA)"
A "Token del BFF movil: emitido, solo con el scope cuentas.leer"

Sub "Sin token"
Mostrar "GET" "/api/v1/clientes" $null $null | Out-Null

Sub "Token valido pero sin el scope necesario: el movil intenta depositar"
Mostrar "POST" "/api/v1/pagos/depositos" "{`"cuentaId`":101,`"monto`":1000,`"referencia`":`"MOVIL-$marca`"}" $tokMovil | Out-Null

# ---------------------------------------------------------------------------
Titulo "4. GESTION DE CLIENTES"
$cuerpoRut = "2" + $marca.PadLeft(7, '0')
$rut = "$cuerpoRut-$(DV $cuerpoRut)"

Sub "Alta de un cliente. El RUT se valida con su digito verificador"
$r = Mostrar "POST" "/api/v1/clientes" "{`"rut`":`"$rut`",`"nombre`":`"Camila Rojas`",`"email`":`"camila.rojas@correo.cl`",`"telefono`":`"+56912345678`",`"segmento`":`"PERSONA`"}" $tok
$clienteId = ($r.cuerpo | ConvertFrom-Json).id

Sub "Mismo RUT otra vez"
Mostrar "POST" "/api/v1/clientes" "{`"rut`":`"$rut`",`"nombre`":`"Duplicado`"}" $tok | Out-Null

Sub "RUT con digito verificador incorrecto"
Mostrar "POST" "/api/v1/clientes" "{`"rut`":`"12345678-9`",`"nombre`":`"Prueba`"}" $tok | Out-Null

Sub "Actualizacion de perfil"
Mostrar "PUT" "/api/v1/clientes/$clienteId" "{`"nombre`":`"Camila Rojas Soto`",`"email`":`"camila.rojas@correo.cl`",`"direccion`":`"Av. Providencia 1234`"}" $tok | Out-Null

# ---------------------------------------------------------------------------
Titulo "5. GESTION DE CUENTAS"
Sub "Apertura. servicio-cuentas valida al titular llamando a servicio-clientes por Eureka"
$r = Mostrar "POST" "/api/v1/cuentas" "{`"clienteId`":$clienteId,`"tipo`":`"ahorro`",`"depositoInicial`":50000}" $tok
$cuentaNueva = ($r.cuerpo | ConvertFrom-Json).cuentaId
A ""
A "La cuenta abierta en linea parte en 100000: el batch nunca toca ese rango."

Sub "Apertura para un cliente que no existe"
Mostrar "POST" "/api/v1/cuentas" "{`"clienteId`":999999,`"tipo`":`"ahorro`"}" $tok | Out-Null

Sub "Una cuenta migrada por el batch, vista desde el mismo servicio"
Mostrar "GET" "/api/v1/cuentas/101" $null $tok | Out-Null

# ---------------------------------------------------------------------------
Titulo "6. PROCESAMIENTO DE PAGOS"
Sub "Deposito"
Mostrar "POST" "/api/v1/pagos/depositos" "{`"cuentaId`":$cuentaNueva,`"monto`":20000,`"referencia`":`"DEP-$marca`",`"glosa`":`"Deposito en efectivo`"}" $tok | Out-Null

Sub "Transferencia de la cuenta nueva a la cuenta migrada 101"
Mostrar "POST" "/api/v1/pagos/transferencias" "{`"cuentaOrigen`":$cuentaNueva,`"cuentaDestino`":101,`"monto`":15000,`"referencia`":`"TRF-$marca`",`"glosa`":`"Arriendo`"}" $tok | Out-Null

Sub "La misma transferencia repetida, con la misma referencia"
Mostrar "POST" "/api/v1/pagos/transferencias" "{`"cuentaOrigen`":$cuentaNueva,`"cuentaDestino`":101,`"monto`":15000,`"referencia`":`"TRF-$marca`",`"glosa`":`"Arriendo`"}" $tok | Out-Null
A ""
A "HTTP 200 con duplicado:true y el mismo saldo: no se debito dos veces."

Sub "Pago de un servicio"
Mostrar "POST" "/api/v1/pagos/servicios" "{`"cuentaId`":$cuentaNueva,`"monto`":12990,`"referencia`":`"PAG-$marca`",`"convenio`":`"Electricidad`"}" $tok | Out-Null

Sub "Transferencia sin saldo suficiente"
Mostrar "POST" "/api/v1/pagos/transferencias" "{`"cuentaOrigen`":$cuentaNueva,`"cuentaDestino`":101,`"monto`":999999,`"referencia`":`"TRF-SS-$marca`"}" $tok | Out-Null

Sub "Saldo de la cuenta tras las operaciones"
Mostrar "GET" "/api/v1/cuentas/$cuentaNueva" $null $tok | Out-Null

# ---------------------------------------------------------------------------
Titulo "7. BALANCEO DE CARGA"
A "Seis consultas seguidas al mismo endpoint. La cabecera X-Atendido-Por"
A "la agrega cada instancia de servicio-pagos al responder."
A ""
for ($i = 1; $i -le 6; $i++) {
    $cab = curl.exe -sk -D - -o NUL -H "Authorization: Bearer $tok" "$G/api/v1/pagos?cuentaId=$cuentaNueva" |
        Select-String "X-Atendido-Por" | ForEach-Object { ($_ -split ":\s*", 2)[1].Trim() }
    A ("  peticion {0}  ->  {1}" -f $i, $cab)
}

# ---------------------------------------------------------------------------
Titulo "8. ALERTAS DE SEGURIDAD"
Sub "Mantenimiento: se bloquea la cuenta"
Mostrar "PATCH" "/api/v1/cuentas/$cuentaNueva" "{`"estado`":`"BLOQUEADA`"}" $tok | Out-Null

Sub "Intento de deposito sobre la cuenta bloqueada"
Mostrar "POST" "/api/v1/pagos/depositos" "{`"cuentaId`":$cuentaNueva,`"monto`":5000,`"referencia`":`"DEP-BLQ-$marca`"}" $tok | Out-Null
A ""
A "Se rechaza y ademas se publica una alerta en el topico banco.eventos.alertas."

Sub "Se desbloquea"
Mostrar "PATCH" "/api/v1/cuentas/$cuentaNueva" "{`"estado`":`"ACTIVA`"}" $tok | Out-Null

Sub "Deposito sobre el umbral de 1.000.000"
Mostrar "POST" "/api/v1/pagos/depositos" "{`"cuentaId`":102,`"monto`":1500000,`"referencia`":`"DEP-ALTO-$marca`"}" $tok | Out-Null
A ""
A "Se aplica, pero tambien genera una alerta para revision."

Start-Sleep -Seconds 3
Sub "Alertas recibidas por servicio-notificaciones"
$al = Llamar "GET" "/api/v1/alertas?cantidad=5" $null $tok
foreach ($a in ($al.cuerpo | ConvertFrom-Json)) {
    A ("  [{0}] {1,-20} cuenta {2,-7} {3}" -f $a.severidad, $a.tipo, $a.cuentaId, $a.detalle)
}

# ---------------------------------------------------------------------------
Titulo "9. EVENTOS DE TRANSACCIONES COMPLETADAS"
A "Cada operacion confirmada publico un evento en banco.eventos.transaccion."
A "servicio-notificaciones los consume y genera el aviso al cliente:"
A ""
$no = Llamar "GET" "/api/v1/notificaciones?cantidad=8" $null $tok
foreach ($n in ($no.cuerpo | ConvertFrom-Json)) {
    A ("  {0,-24} {1}" -f $n.titulo, $n.mensaje)
}

# ---------------------------------------------------------------------------
Titulo "10. CIERRE DE CUENTA"
Sub "Cierre con saldo pendiente"
Mostrar "POST" "/api/v1/cuentas/$cuentaNueva/cierre" $null $tok | Out-Null

$saldo = ((Llamar "GET" "/api/v1/cuentas/$cuentaNueva" $null $tok).cuerpo | ConvertFrom-Json).saldo
Sub "Se transfiere el saldo restante ($saldo) a otra cuenta"
Mostrar "POST" "/api/v1/pagos/transferencias" "{`"cuentaOrigen`":$cuentaNueva,`"cuentaDestino`":101,`"monto`":$saldo,`"referencia`":`"TRF-CIERRE-$marca`"}" $tok | Out-Null

Sub "Cierre con saldo cero"
Mostrar "POST" "/api/v1/cuentas/$cuentaNueva/cierre" $null $tok | Out-Null

# ---------------------------------------------------------------------------
Titulo "11. TOLERANCIA A FALLOS"
A "Se detiene servicio-clientes y se intenta abrir cuentas."
$proc = Get-NetTCPConnection -LocalPort 8085 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique
if ($proc) { Stop-Process -Id $proc -Force }
Start-Sleep -Seconds 4

for ($i = 1; $i -le 6; $i++) {
    $r = Llamar "POST" "/api/v1/cuentas" "{`"clienteId`":1,`"tipo`":`"ahorro`"}" $tok
    $msg = try { ($r.cuerpo | ConvertFrom-Json).mensaje } catch { $r.cuerpo }
    A ("  intento {0}: HTTP {1}  {2}" -f $i, $r.codigo, $msg)
}
A ""
A "> GET http://localhost:8086/actuator/circuitbreakers"
$cb = Invoke-RestMethod "http://localhost:8086/actuator/circuitbreakers"
$c = $cb.circuitBreakers.clientes
A ("  circuito clientes: estado={0}  fallas={1}  tasa={2}  rechazadas sin llamar={3}" -f `
    $c.state, $c.failedCalls, $c.failureRate, $c.notPermittedCalls)
A ""
A "Comportamiento alternativo: sin poder confirmar al titular, la cuenta no se"
A "abre y el cliente recibe un 503 claro. Con el circuito abierto ni siquiera se"
A "intenta la llamada: se responde de inmediato."

A ""
A "Se vuelve a levantar servicio-clientes."
$jar = Join-Path $raiz "servicio-clientes\target\servicio-clientes-1.0-SNAPSHOT.jar"
$env:PORT = "8085"
Start-Process -FilePath "java" -ArgumentList "-Xms64m", "-Xmx320m", "-jar", $jar -WorkingDirectory $raiz `
    -RedirectStandardOutput (Join-Path $raiz "logs\servicio-clientes-8085.log") `
    -RedirectStandardError (Join-Path $raiz "logs\servicio-clientes-8085.err.log") -WindowStyle Hidden | Out-Null
Remove-Item Env:\PORT -ErrorAction SilentlyContinue
$fin = (Get-Date).AddSeconds(90)
do { Start-Sleep -Seconds 3; $ok = curl.exe -s -o NUL -w "%{http_code}" http://localhost:8085/actuator/health } while ($ok -ne "200" -and (Get-Date) -lt $fin)
A "  servicio-clientes de vuelta (HTTP $ok)."

# ---------------------------------------------------------------------------
Titulo "12. MONITOREO"
A "> GET http://localhost:8087/actuator/prometheus  (extracto)"
curl.exe -s http://localhost:8087/actuator/prometheus |
    Select-String '^http_server_requests_seconds_count\{.*uri="/api/v1/pagos' |
    Select-Object -First 4 | ForEach-Object { A ("  " + $_.Line) }
A ""
A "Cada servicio expone metricas en formato Prometheus y cada linea de log lleva"
A "el traceId de la peticion, que viaja entre servicios. Extracto del log de"
A "servicio-cuentas durante la apertura:"
Get-Content (Join-Path $raiz "logs\servicio-cuentas-8086.log") |
    Select-String "Cuenta abierta" | Select-Object -Last 2 | ForEach-Object { A ("  " + $_.Line) }

$l | Set-Content -Path $salida -Encoding UTF8
Write-Host ""
Write-Host "escrito: $salida ($($l.Count) lineas)"
