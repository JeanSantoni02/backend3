package com.bank.xyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;


@Component
public class MetricasJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MetricasJobListener.class);
    private static final String LINEA = "=".repeat(96);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(LINEA);
        log.info("INICIO JOB: {}  | parametros: {}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
        log.info(LINEA);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duracion = Duration.ZERO;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            duracion = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime());
        }

        long leidasTotal = 0;
        long escritasTotal = 0;
        long descartadasTotal = 0;

        log.info(LINEA);
        log.info("FIN JOB: {}  | estado: {}  | duracion: {} ms",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                duracion.toMillis());
        log.info("{}", "-".repeat(96));
        log.info(String.format("%-34s %8s %8s %8s %8s %8s",
                "STEP", "LEIDAS", "ESCRITAS", "SKIP-L", "SKIP-P", "SKIP-E"));

        for (StepExecution step : jobExecution.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getStepName))
                .toList()) {

            // El step maestro ya acumula los contadores de sus particiones, asi que
            // los steps hijos (nombre con ":particion-N") se listan pero no se suman.
            if (!esParticion(step.getStepName())) {
                leidasTotal += step.getReadCount();
                escritasTotal += step.getWriteCount();
                descartadasTotal += step.getReadSkipCount()
                        + step.getProcessSkipCount() + step.getWriteSkipCount();
            }

            log.info(String.format("%-34s %8d %8d %8d %8d %8d",
                    recortar(step.getStepName()),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getReadSkipCount(),
                    step.getProcessSkipCount(),
                    step.getWriteSkipCount()));
        }

        log.info("{}", "-".repeat(96));
        log.info("TOTAL leidas={}  escritas={}  descartadas={}  (steps ejecutados: {})",
                leidasTotal, escritasTotal, descartadasTotal, jobExecution.getStepExecutions().size());

        if (!jobExecution.getAllFailureExceptions().isEmpty()) {
            log.error("El job termino con {} excepcion(es):", jobExecution.getAllFailureExceptions().size());
            jobExecution.getAllFailureExceptions()
                    .forEach(e -> log.error("   -> {}: {}", e.getClass().getSimpleName(), e.getMessage()));
        }
        log.info(LINEA);
    }

    private boolean esParticion(String nombreStep) {
        return nombreStep.contains(":");
    }

    private String recortar(String nombre) {
        return nombre.length() <= 34 ? nombre : nombre.substring(nombre.length() - 34);
    }
}
