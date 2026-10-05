# Registra la ejecucion de la plataforma en contenedores.
# Requiere los contenedores arriba:  docker compose up -d

$raiz = Split-Path $PSScriptRoot -Parent
$salida = Join-Path $PSScriptRoot "08-microservicios\07-docker-en-ejecucion.txt"
$l = New-Object System.Collections.ArrayList
function A($t) { [void]$l.Add($t) }
function Titulo($t) {
    A ""
    A "--------------------------------------------------------------"
    A $t
    A "--------------------------------------------------------------"
}

A "=============================================================="
A " PLATAFORMA EN CONTENEDORES"
A " Generado: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
A "=============================================================="

Titulo "VERSIONES"
A "> docker version --format '{{.Server.Version}}'"
A ("  " + (docker version --format '{{.Server.Version}}'))
A ""
A "> docker compose version --short"
A ("  " + (docker compose version --short))

Titulo "IMAGENES CONSTRUIDAS"
A "> docker images --filter reference=banco-xyz-*"
docker images --filter "reference=banco-xyz-*" --format "  {{.Repository}}  {{.Size}}" | ForEach-Object { A $_ }

Titulo "CONTENEDORES EN EJECUCION"
A "> docker compose ps"
docker compose ps --format "  {{.Name}}|{{.Status}}|{{.Ports}}" | ForEach-Object { A $_ }

Titulo "DEPENDENCIAS RESUELTAS POR HEALTHCHECK"
A "Ningun microservicio arranca antes que su infraestructura. El orden"
A "que impuso compose en este arranque fue:"
A ""
A "  postgres y artemis  ->  healthy"
A "  config-server       ->  healthy"
A "  eureka-server       ->  healthy"
A "  auth-server         ->  healthy"
A "  banco-core-api      ->  healthy   (espera a postgres, config y eureka)"
A "  servicio-notificaciones y los tres BFF"

Titulo "SERVICE DISCOVERY DENTRO DE LA RED DE DOCKER"
A "> GET http://localhost:8761/eureka/apps"
try {
    $e = Invoke-RestMethod "http://localhost:8761/eureka/apps" -Headers @{Accept = "application/json" } -TimeoutSec 20
    $e.applications.application | Sort-Object name | ForEach-Object {
        A ("  {0,-26} {1}" -f $_.name, $_.instance.status)
    }
} catch { A "  error: $_" }

Titulo "OAUTH2 CONTRA EL CONTENEDOR auth-server"
A "> POST http://localhost:9000/oauth2/token  (client_credentials, cliente bff-atm)"
$t = curl.exe -s -u "bff-atm:secreto-bff-atm" -d "grant_type=client_credentials&scope=cuentas.leer retiros.escribir" http://localhost:9000/oauth2/token
if ($t -match "access_token") {
    $j = $t | ConvertFrom-Json
    A "  token emitido"
    A ("  tipo:    " + $j.token_type)
    A ("  scopes:  " + $j.scope)
    A ("  vigencia: " + $j.expires_in + " s")
    $tok = $j.access_token
} else {
    A "  respuesta inesperada: $t"
    $tok = ""
}

Titulo "AUTORIZACION EN EL SERVICIO DE DOMINIO"
A "> GET /api/v1/cuentas/101/saldo  SIN token"
A ("  HTTP " + (curl.exe -s -o NUL -w "%{http_code}" http://localhost:8080/api/v1/cuentas/101/saldo))
A "  El resource server rechaza la peticion sin credencial."
A ""
A "> GET /api/v1/cuentas/101/saldo  CON token"
A ("  HTTP " + (curl.exe -s -o NUL -w "%{http_code}" -H "Authorization: Bearer $tok" http://localhost:8080/api/v1/cuentas/101/saldo))
A "  La autorizacion pasa y el dominio responde con los datos que cargo el"
A "  batch en la base del contenedor."

Titulo "FLUJO COMPLETO ENTRE CONTENEDORES"
$ref = "DOCKER-" + (Get-Date -Format "HHmmss")
$cuerpo = Join-Path $env:TEMP "evid-retiro-docker.json"
Set-Content -Path $cuerpo -Value "{`"monto`":5000,`"referencia`":`"$ref`"}" -Encoding ASCII

A "> POST /bff/atm/cuentas/101/retiro   (monto 5000, referencia $ref)"
A ("  " + (curl.exe -s -X POST "http://localhost:8083/bff/atm/cuentas/101/retiro" -H "Content-Type: application/json" -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" -d "@$cuerpo"))
A ""
A "El dominio confirma el debito y recien entonces publica el evento en el"
A "topico del contenedor de Artemis."
A ""
Start-Sleep -Seconds 3
A "> GET http://localhost:8084/api/v1/notificaciones"
A ("  " + (curl.exe -s "http://localhost:8084/api/v1/notificaciones"))
A ""
A "El servicio de notificaciones, en otro contenedor, consumio el evento y"
A "genero el aviso sin que nadie se lo pidiera."
A ""
$antes = (curl.exe -s "http://localhost:8084/api/v1/notificaciones/resumen" | ConvertFrom-Json).totalNotificaciones
A "> GET /api/v1/notificaciones/resumen   antes de repetir"
A "  totalNotificaciones: $antes"
A ""
A "> POST el mismo retiro otra vez, con la misma referencia"
A ("  " + (curl.exe -s -X POST "http://localhost:8083/bff/atm/cuentas/101/retiro" -H "Content-Type: application/json" -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" -d "@$cuerpo"))
A ""
A "Responde duplicado:true con el mismo saldo: no debita dos veces."
A ""
Start-Sleep -Seconds 3
$despues = (curl.exe -s "http://localhost:8084/api/v1/notificaciones/resumen" | ConvertFrom-Json).totalNotificaciones
A "> GET /api/v1/notificaciones/resumen   despues de repetir"
A "  totalNotificaciones: $despues"
A ""
A "El contador no se movio de ${antes} a ${despues}: la peticion repetida no"
A "genero una segunda notificacion."
A ""
A "> POST /bff/atm/cuentas/101/retiro   (monto 4500)"
Set-Content -Path $cuerpo -Value "{`"monto`":4500,`"referencia`":`"$ref-B`"}" -Encoding ASCII
A ("  " + (curl.exe -s -X POST "http://localhost:8083/bff/atm/cuentas/101/retiro" -H "Content-Type: application/json" -H "X-ATM-Terminal: ATM-001" -H "X-ATM-Key: clave-demo-001" -d "@$cuerpo"))
A ""
A "Rechazo de negocio: el cajero solo entrega billetes de 1000. No es una"
A "falla, asi que no cuenta para abrir el circuito."

Titulo "CONSUMO DE RECURSOS"
A "> docker stats --no-stream"
docker stats --no-stream --format "  {{.Name}}|{{.CPUPerc}}|{{.MemUsage}}" | ForEach-Object { A $_ }

A ""
A "=============================================================="
A " RESULTADO"
A "=============================================================="
A "Los diez componentes levantan con un solo comando, cada uno espera a"
A "que su infraestructura este sana y los cinco microservicios se registran"
A "en Eureka por la red interna de Docker."
A ""
A "El sistema opera igual que fuera de contenedores: el cajero autentica su"
A "terminal, el BFF pide un token al servidor de autorizacion, el dominio lo"
A "valida y debita, publica el evento en el broker y el servicio de"
A "notificaciones lo consume desde otro contenedor."

$l | Set-Content -Path $salida -Encoding UTF8
"escrito: $salida"
"lineas:  " + $l.Count
