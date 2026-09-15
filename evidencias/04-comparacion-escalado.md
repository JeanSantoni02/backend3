# Evidencia de la estrategia de escalado

Comparación de la misma carga de trabajo (los tres procesos, 1.000 filas por archivo)
ejecutada con 1 partición y con 4 particiones. El único cambio entre ambas corridas es
el argumento `--banco.batch.particiones`, sin recompilar.

```bash
java -jar target/batch-migration-1.0-SNAPSHOT.jar --job=todos --banco.batch.particiones=1
java -jar target/batch-migration-1.0-SNAPSHOT.jar --job=todos --banco.batch.particiones=4
```

Equipo: Windows 11, JDK 17 (Temurin), PostgreSQL 18 local, chunk = 50 filas.

## Resultado

| Proceso | 1 partición | 4 particiones | Mejora |
|---|---:|---:|---:|
| Reporte de transacciones diarias | 1.386 ms | 1.053 ms | 24 % |
| Cálculo de intereses mensuales | 801 ms | 453 ms | 43 % |
| Estados de cuenta anuales | 599 ms | 269 ms | 55 % |
| **Total** | **2.786 ms** | **1.775 ms** | **36 %** |

El primer proceso mejora menos porque absorbe el calentamiento de la JVM y la
inicialización del pool de conexiones. Los dos siguientes, ya con todo caliente,
muestran el rendimiento real del paralelismo.

## Prueba de que el paralelismo se ejercita de verdad

El particionador cuenta las filas reales del archivo y reparte rangos disjuntos:

```
particion-0 -> salta 1 lineas y procesa 250 filas de transacciones.csv
particion-1 -> salta 251 lineas y procesa 250 filas de transacciones.csv
particion-2 -> salta 501 lineas y procesa 250 filas de transacciones.csv
particion-3 -> salta 751 lineas y procesa 250 filas de transacciones.csv
```

Cada partición corre en un hilo distinto del pool (`particion-1` a `particion-4`) y
termina en un momento distinto, lo que confirma ejecución concurrente y no secuencial:

```
[particion-4] Step: [stepCargaTransacciones:particion-2] executed in 924ms
[particion-1] Step: [stepCargaTransacciones:particion-1] executed in 939ms
[particion-3] Step: [stepCargaTransacciones:particion-0] executed in 991ms
[particion-2] Step: [stepCargaTransacciones:particion-3] executed in 1s8ms
```

Spring Batch registra cada partición como un `StepExecution` independiente, y el step
maestro consolida los totales:

```
STEP                                 LEIDAS ESCRITAS   SKIP-L   SKIP-P   SKIP-E
stepCargaTransacciones:particion-0      250      210        0       40        0
stepCargaTransacciones:particion-1      250      185        0       65        0
stepCargaTransacciones:particion-2      250      197        0       53        0
stepCargaTransacciones:particion-3      250      193        0       57        0
stepCargaTransaccionesMaestro          1000      785        0      215        0
stepResumenDiario                         0      322        0        0        0
```

250 × 4 = 1.000 filas leídas, 785 escritas y 215 descartadas: los totales cuadran y
ninguna fila se lee dos veces ni se pierde.

Lo mismo se puede verificar directamente en las tablas de metadatos:

```sql
SELECT se.step_name, se.status, se.read_count, se.write_count, se.process_skip_count
FROM batch_step_execution se
WHERE se.job_execution_id = (SELECT max(job_execution_id) FROM batch_job_execution)
ORDER BY se.step_execution_id;
```

## Resumen por proceso, corrida con 4 particiones

| Proceso | Leídas | Escritas (detalle) | Escritas (resumen) | Descartadas |
|---|---:|---:|---:|---:|
| Transacciones diarias | 1.000 | 785 | 322 | 215 |
| Intereses mensuales | 1.000 | 526 | 50 | 474 |
| Estados de cuenta anuales | 1.000 | 952 | 20 | 48 |
