# Despliegue en la nube (AWS)

Este documento explica cómo llevar la plataforma del Banco XYZ a AWS. Hay dos caminos:

| | Opción A — EC2 con Docker Compose | Opción B — ECS Fargate con servicios administrados |
|---|---|---|
| Para qué | Demostración, ambiente de pruebas | Producción |
| Esfuerzo | Una hora | Uno a dos días |
| Cambios en el código | Ninguno | Ninguno; solo configuración por variables de entorno |
| Escalado | Vertical (instancia más grande) | Horizontal y automático por servicio |
| Disponibilidad | Una sola máquina | Dos zonas de disponibilidad |

Los dos usan las mismas imágenes Docker que ya construye el `docker-compose.yml`.

---

## Arquitectura objetivo (Opción B)

```
                         Internet
                            │ HTTPS 443
                            ▼
                ┌───────────────────────┐
                │ Application Load      │  certificado de ACM
                │ Balancer (público)    │
                └───────────┬───────────┘
   ─────────────────────────┼──────────────────────── subredes privadas
                            ▼
                ┌───────────────────────┐
                │  api-gateway (ECS)    │ 2 tareas
                └───────────┬───────────┘
          ┌─────────┬───────┼─────────┬──────────┐
          ▼         ▼       ▼         ▼          ▼
     clientes   cuentas   pagos     BFFs    notificaciones      ECS Fargate
                          (2..6)
          │         │       │         │          │
          └────┬────┴───┬───┴─────────┴──────────┘
               ▼        ▼
        ┌───────────┐ ┌────────────────┐ ┌────────────┐
        │ RDS       │ │ Broker Artemis │ │ config,    │
        │ PostgreSQL│ │ (ECS + EFS)    │ │ eureka,    │
        │ Multi-AZ  │ │                │ │ auth (ECS) │
        └───────────┘ └────────────────┘ └────────────┘

  Transversal: ECR (imágenes) · Secrets Manager (credenciales) · CloudWatch (logs y métricas)
```

### Correspondencia entre componentes

| Componente | En local | En AWS |
|---|---|---|
| Imágenes | Se construyen en la máquina | Amazon ECR, un repositorio por servicio |
| Microservicios | Procesos Java o contenedores | ECS Fargate, un servicio ECS por microservicio |
| PostgreSQL | Instalado o contenedor | Amazon RDS for PostgreSQL, Multi-AZ |
| Broker JMS | Artemis embebido o contenedor | Contenedor de Artemis en ECS con volumen EFS |
| Configuración | Carpeta `config-repo` | Config Server leyendo un repositorio Git |
| Descubrimiento | Eureka | Eureka en ECS (cada tarea tiene su propia IP) |
| Certificado TLS | Autofirmado en el gateway | AWS Certificate Manager en el balanceador |
| Credenciales | Variables de entorno | AWS Secrets Manager |
| Logs y métricas | Consola y `/actuator` | CloudWatch Logs y Container Insights |
| Carga del batch | `java -jar` | Tarea ECS programada con EventBridge |

---

## Requisitos previos

- Cuenta de AWS con permisos sobre ECR, ECS, RDS, EC2, IAM, Secrets Manager y CloudWatch.
- AWS CLI v2 configurado:

```bash
aws configure
```

- Docker y Maven en la máquina desde la que se publican las imágenes.
- Región: los comandos usan `us-east-1`; se puede cambiar por `sa-east-1` (São Paulo), la más cercana a Chile.

Variables usadas en los comandos (PowerShell):

```bash
$REGION = "us-east-1"; $CUENTA = (aws sts get-caller-identity --query Account --output text)
```

```bash
$ECR = "$CUENTA.dkr.ecr.$REGION.amazonaws.com"
```

---

## Paso 1 — Publicar las imágenes en ECR

Crear un repositorio por servicio:

```bash
$servicios = "config-server","eureka-server","auth-server","banco-core-api","servicio-notificaciones","servicio-clientes","servicio-cuentas","servicio-pagos","bff-web","bff-mobile","bff-atm","api-gateway"
```

```bash
foreach ($s in $servicios) { aws ecr create-repository --repository-name "banco-xyz/$s" --region $REGION --image-scanning-configuration scanOnPush=true }
```

Construir con el compose del proyecto y autenticarse en ECR:

```bash
docker compose build
```

```bash
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ECR
```

Etiquetar y subir cada imagen:

```bash
foreach ($s in $servicios) { docker tag "banco-xyz-$s" "$ECR/banco-xyz/${s}:1.0"; docker push "$ECR/banco-xyz/${s}:1.0" }
```

`scanOnPush` hace que ECR revise cada imagen en busca de vulnerabilidades conocidas al subirla.

---

## Opción A — Una instancia EC2 con Docker Compose

La forma más directa de tener el sistema en la nube.

1. **Lanzar la instancia.** Amazon Linux 2023, tipo `t3.xlarge` (4 vCPU, 16 GB): los quince contenedores usan alrededor de 4 GB.
2. **Grupo de seguridad.** Entrada solo por el puerto 8443 (gateway) y 22 (SSH) desde la IP del administrador. Los demás puertos no se abren: los servicios se hablan por la red interna de Docker.
3. **Instalar Docker y Git:**

```bash
sudo dnf install -y docker git && sudo systemctl enable --now docker && sudo usermod -aG docker ec2-user
```

```bash
sudo mkdir -p /usr/local/lib/docker/cli-plugins && sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose && sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
```

4. **Clonar y configurar credenciales.** En lugar de dejarlas por defecto, se definen en un archivo `.env` que no se versiona:

```bash
git clone https://github.com/JeanSantoni02/backend3.git && cd backend3
```

```bash
cat > .env <<'EOF'
DB_PASSWORD=una-clave-larga-y-aleatoria
PORTAL_OPERACIONES_SECRET=otra-clave
SERVICIO_CUENTAS_SECRET=otra-clave
BFF_WEB_SECRET=otra-clave
BFF_MOBILE_SECRET=otra-clave
BFF_ATM_SECRET=otra-clave
TLS_KEYSTORE_PASSWORD=cambiar-en-produccion
EOF
```

5. **Cargar los datos y levantar.** El orden importa: el batch va antes que el resto (ver `instrucciones.md`):

```bash
docker run --rm -v "$PWD":/proyecto -w /proyecto maven:3.9-eclipse-temurin-17 mvn -q -B -pl batch-migration package -DskipTests
```

```bash
docker compose up -d postgres
```

```bash
docker run --rm --network banco-xyz_banco -e DB_URL=jdbc:postgresql://postgres:5432/bank_legacy_db -e DB_PASSWORD=$(grep DB_PASSWORD .env | cut -d= -f2) -v "$PWD/batch-migration/target:/app" eclipse-temurin:17-jre java -jar /app/batch-migration-1.0-SNAPSHOT.jar --job=todos
```

```bash
docker compose up -d
```

6. **Verificar:**

```bash
docker compose ps
```

```bash
curl -k https://IP-PUBLICA:8443/actuator/health
```

Limitaciones de esta opción: un solo punto de falla, certificado autofirmado y escalado manual. Sirve para mostrar el sistema funcionando en la nube, no para operar un banco.

---

## Opción B — ECS Fargate

### Paso 2 — Red

Una VPC con dos zonas de disponibilidad, cada una con una subred pública y una privada.
Desde la consola: **VPC → Crear VPC → "VPC y más"**, con 2 zonas, 2 subredes públicas,
2 privadas y 1 NAT Gateway. El asistente crea también las tablas de rutas.

| Subred | Qué vive ahí |
|---|---|
| Pública | El balanceador y el NAT Gateway |
| Privada | Todas las tareas ECS, RDS y el broker |

Grupos de seguridad, de afuera hacia adentro:

| Grupo | Acepta | Desde |
|---|---|---|
| `sg-alb` | 443 | Internet |
| `sg-gateway` | 8443 | `sg-alb` |
| `sg-servicios` | 8080–8888, 9000 | `sg-gateway` y `sg-servicios` |
| `sg-datos` | 5432 y 61616 | `sg-servicios` |

Nada de la capa privada es alcanzable desde internet: solo el balanceador tiene IP pública.

### Paso 3 — Credenciales en Secrets Manager

```bash
aws secretsmanager create-secret --name banco-xyz/db-password --secret-string "una-clave-larga-y-aleatoria"
```

```bash
aws secretsmanager create-secret --name banco-xyz/oauth-clientes --secret-string '{"PORTAL_OPERACIONES_SECRET":"...","SERVICIO_CUENTAS_SECRET":"...","BFF_WEB_SECRET":"...","BFF_MOBILE_SECRET":"...","BFF_ATM_SECRET":"..."}'
```

Las definiciones de tarea los leen con el bloque `secrets` (ver paso 7). Nunca quedan en la imagen ni en el repositorio.

### Paso 4 — Base de datos

```bash
aws rds create-db-instance --db-instance-identifier banco-xyz-db --engine postgres --engine-version 16 --db-instance-class db.t3.medium --allocated-storage 50 --master-username postgres --manage-master-user-password --db-name bank_legacy_db --multi-az --no-publicly-accessible --vpc-security-group-ids SG_DATOS --db-subnet-group-name banco-xyz-privadas --backup-retention-period 7 --storage-encrypted
```

- `--multi-az` mantiene una réplica en otra zona que toma el control si la principal falla.
- `--manage-master-user-password` deja la contraseña en Secrets Manager.
- `--storage-encrypted` cifra los datos en reposo.

### Paso 5 — Broker de mensajería

**Recomendado:** el mismo contenedor de Artemis que usa el compose, como servicio ECS con un volumen EFS para que los mensajes persistan si la tarea se reinicia. No requiere cambios en el código.

**Alternativa administrada:** Amazon MQ. Ofrece ActiveMQ Classic, no Artemis, así que habría que cambiar en los `pom.xml` la dependencia `spring-boot-starter-artemis` por `spring-boot-starter-activemq`. El código no cambia, porque todo usa `JmsTemplate` y `@JmsListener`.

### Paso 6 — Configuración centralizada

En la nube, el Config Server deja el perfil `native` y lee de un repositorio Git, de modo que cambiar un umbral es hacer un commit:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `git` |
| `SPRING_CLOUD_CONFIG_SERVER_GIT_URI` | URL del repositorio de configuración |
| `SPRING_CLOUD_CONFIG_SERVER_GIT_SEARCH_PATHS` | `config-repo` |

El contenido de `config-server/src/main/resources/config-repo/` se mueve a ese repositorio tal cual.

### Paso 7 — Cluster y definiciones de tarea

```bash
aws ecs create-cluster --cluster-name banco-xyz --settings name=containerInsights,value=enabled
```

```bash
aws logs create-log-group --log-group-name /ecs/banco-xyz
```

Una definición de tarea por servicio. Ejemplo para `servicio-pagos` (`ecs/servicio-pagos.json`):

```json
{
  "family": "servicio-pagos",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::CUENTA:role/ecsTaskExecutionRole",
  "containerDefinitions": [{
    "name": "servicio-pagos",
    "image": "CUENTA.dkr.ecr.REGION.amazonaws.com/banco-xyz/servicio-pagos:1.0",
    "portMappings": [{ "containerPort": 8087 }],
    "environment": [
      { "name": "CONFIG_URL", "value": "http://config-server.banco.local:8888" },
      { "name": "EUREKA_URL", "value": "http://eureka-server.banco.local:8761/eureka/" },
      { "name": "AUTH_URL",   "value": "http://auth-server.banco.local:9000" },
      { "name": "DB_URL",     "value": "jdbc:postgresql://ENDPOINT-RDS:5432/bank_legacy_db" },
      { "name": "ARTEMIS_URL","value": "tcp://artemis.banco.local:61616" },
      { "name": "JAVA_TOOL_OPTIONS", "value": "-XX:MaxRAMPercentage=75" }
    ],
    "secrets": [
      { "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:REGION:CUENTA:secret:banco-xyz/db-password" }
    ],
    "healthCheck": {
      "command": ["CMD-SHELL", "curl -sf http://localhost:8087/actuator/health || exit 1"],
      "interval": 15, "timeout": 5, "retries": 5, "startPeriod": 90
    },
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/banco-xyz",
        "awslogs-region": "REGION",
        "awslogs-stream-prefix": "servicio-pagos"
      }
    }
  }]
}
```

```bash
aws ecs register-task-definition --cli-input-json file://ecs/servicio-pagos.json
```

Los nombres `*.banco.local` los resuelve **ECS Service Connect** (o Cloud Map) para los servicios de plataforma, que necesitan una dirección fija antes de que Eureka exista. Entre microservicios se sigue usando Eureka, como en local.

`-XX:MaxRAMPercentage=75` hace que la JVM dimensione el heap según la memoria de la tarea, en vez de la del servidor físico.

### Paso 8 — Servicios ECS, en orden

El orden replica el de los `depends_on` del compose:

1. `config-server`, `eureka-server`, `auth-server`, `artemis`
2. `banco-core-api`, `servicio-clientes`, `servicio-cuentas`, `servicio-pagos`, `servicio-notificaciones`
3. `bff-web`, `bff-mobile`, `bff-atm`
4. `api-gateway`, asociado al balanceador

```bash
aws ecs create-service --cluster banco-xyz --service-name servicio-pagos --task-definition servicio-pagos --desired-count 2 --launch-type FARGATE --network-configuration "awsvpcConfiguration={subnets=[PRIV_A,PRIV_B],securityGroups=[SG_SERVICIOS],assignPublicIp=DISABLED}"
```

Con `--desired-count 2` y dos subredes, ECS reparte las tareas entre las dos zonas.

**`auth-server` va con una sola tarea.** Genera su par de claves RSA al arrancar: dos instancias firmarían con claves distintas y un servicio rechazaría los tokens de la otra. Para escalarlo hay que mover la clave a Secrets Manager (ver mejoras).

### Paso 9 — Balanceador con HTTPS

1. Pedir el certificado del dominio en **AWS Certificate Manager** y validarlo por DNS.
2. Crear un **Application Load Balancer** en las subredes públicas con un listener HTTPS:443 que use ese certificado.
3. Crear un grupo de destino tipo IP, puerto 8443, protocolo HTTPS, con chequeo de salud en `/actuator/health`.
4. Asociar el servicio `api-gateway` a ese grupo de destino.
5. Agregar un listener HTTP:80 que solo redirija a HTTPS.

El balanceador presenta el certificado público de ACM. Del balanceador al gateway el tráfico va de nuevo cifrado (protocolo HTTPS en el grupo de destino), así que nunca viaja en claro, ni siquiera dentro de la VPC.

### Paso 10 — Escalado automático

```bash
aws application-autoscaling register-scalable-target --service-namespace ecs --resource-id service/banco-xyz/servicio-pagos --scalable-dimension ecs:service:DesiredCount --min-capacity 2 --max-capacity 6
```

```bash
aws application-autoscaling put-scaling-policy --service-namespace ecs --resource-id service/banco-xyz/servicio-pagos --scalable-dimension ecs:service:DesiredCount --policy-name cpu-60 --policy-type TargetTrackingScaling --target-tracking-scaling-policy-configuration '{"TargetValue":60.0,"PredefinedMetricSpecification":{"PredefinedMetricType":"ECSServiceAverageCPUUtilization"},"ScaleInCooldown":120,"ScaleOutCooldown":60}'
```

Cuando la CPU promedio supera el 60 %, ECS agrega tareas. Cada tarea nueva se registra sola en Eureka y el gateway empieza a enviarle tráfico sin configuración adicional: es el mismo mecanismo que se ve en local con las dos instancias de pagos.

### Paso 11 — Carga inicial con el batch

El batch no es un servicio que quede corriendo: es una tarea que se ejecuta y termina.

```bash
aws ecs run-task --cluster banco-xyz --task-definition batch-migration --launch-type FARGATE --network-configuration "awsvpcConfiguration={subnets=[PRIV_A],securityGroups=[SG_SERVICIOS],assignPublicIp=DISABLED}"
```

Para ejecuciones periódicas (por ejemplo, el cierre de intereses de fin de mes) se programa con **EventBridge Scheduler**, que lanza la misma tarea según una expresión cron.

La reejecución automática ante fallos ya está en el código. Si el contenedor completo termina con error, EventBridge puede reintentarlo con su propia política de reintentos.

---

## Monitoreo

| Qué | Dónde |
|---|---|
| Logs de cada servicio | CloudWatch Logs, grupo `/ecs/banco-xyz`, un stream por tarea |
| CPU, memoria y tareas | CloudWatch Container Insights |
| Métricas de la aplicación | `/actuator/prometheus` de cada servicio, recolectado por Amazon Managed Service for Prometheus |
| Seguimiento de una petición | Buscar el `traceId` de la línea de log en CloudWatch Logs Insights |
| Alertas | Alarmas de CloudWatch sobre errores 5xx del balanceador y sobre tareas no saludables |

Consulta de Logs Insights para seguir una petición entre servicios:

```
fields @timestamp, @logStream, @message
| filter @message like /6ac580fe5ae2f374/
| sort @timestamp asc
```

---

## Costos aproximados

Referencia mensual en `us-east-1`, uso continuo:

| Recurso | Configuración | USD / mes |
|---|---|---|
| ECS Fargate | ~16 tareas de 0,5 vCPU y 1 GB | 240 |
| RDS PostgreSQL | db.t3.medium Multi-AZ | 140 |
| Application Load Balancer | 1 | 25 |
| NAT Gateway | 1 | 35 |
| EFS, ECR, Secrets Manager, CloudWatch | uso bajo | 20 |
| **Total Opción B** | | **≈ 460** |
| **Opción A** | t3.xlarge | **≈ 120** |

---

## Retirar el despliegue

Para no seguir pagando después de una demostración:

```bash
aws ecs delete-service --cluster banco-xyz --service servicio-pagos --force
```

(Repetir para cada servicio.) Luego eliminar el cluster, la instancia RDS, el balanceador, el NAT Gateway y los repositorios de ECR, en ese orden.
