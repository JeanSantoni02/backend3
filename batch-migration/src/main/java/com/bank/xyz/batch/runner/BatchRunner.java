package com.bank.xyz.batch.runner;

import com.bank.xyz.batch.config.BatchProperties;
import com.bank.xyz.batch.config.EstadoCuentaAnualJobConfig;
import com.bank.xyz.batch.config.InteresesJobConfig;
import com.bank.xyz.batch.config.TransaccionesJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class BatchRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(BatchRunner.class);

    private static final Map<String, String> ALIAS = Map.of(
            "transacciones", TransaccionesJobConfig.JOB,
            "intereses", InteresesJobConfig.JOB,
            "anuales", EstadoCuentaAnualJobConfig.JOB);

    /** Orden de ejecucion cuando se pide "todos". */
    private static final List<String> ORDEN = List.of("transacciones", "intereses", "anuales");

    private final JobLauncher jobLauncher;
    private final Map<String, Job> jobsPorNombre;
    private final BatchProperties propiedades;

    /** 0 si todos los jobs terminaron bien, 1 si alguno fallo. */
    private int codigoSalida = 0;

    public BatchRunner(JobLauncher jobLauncher, List<Job> jobs, BatchProperties propiedades) {
        this.jobLauncher = jobLauncher;
        this.propiedades = propiedades;
        this.jobsPorNombre = jobs.stream()
                .collect(Collectors.toMap(Job::getName, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String seleccion = args.containsOption("job")
                ? args.getOptionValues("job").get(0).toLowerCase()
                : "todos";

        List<String> aEjecutar = "todos".equals(seleccion) ? ORDEN : List.of(seleccion);

        for (String alias : aEjecutar) {
            String nombreJob = ALIAS.get(alias);
            if (nombreJob == null) {
                log.error("Job desconocido: '{}'. Opciones validas: todos, {}",
                        alias, String.join(", ", ORDEN));
                codigoSalida = 2;
                return;
            }
            ejecutar(nombreJob);
        }
    }

    private void ejecutar(String nombreJob) throws Exception {
        Job job = jobsPorNombre.get(nombreJob);
        if (job == null) {
            log.error("No hay ningun bean Job registrado con el nombre '{}'", nombreJob);
            codigoSalida = 2;
            return;
        }

        JobParameters parametros = new JobParametersBuilder()
                .addLong("ejecucion", System.currentTimeMillis())
                .addLong("particiones", (long) propiedades.getParticiones())
                .addLong("chunkSize", (long) propiedades.getChunkSize())
                .toJobParameters();

        JobExecution ejecucion = jobLauncher.run(job, parametros);

        if (ejecucion.getStatus().isUnsuccessful()) {
            log.error("El job {} termino con estado {}", nombreJob, ejecucion.getStatus());
            codigoSalida = 1;
        }
    }

    @Override
    public int getExitCode() {
        return codigoSalida;
    }
}
