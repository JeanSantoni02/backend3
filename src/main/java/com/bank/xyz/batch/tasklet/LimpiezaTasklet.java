package com.bank.xyz.batch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;


public class LimpiezaTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaTasklet.class);

    private final String tabla;
    private final String jobNombre;
    private final JdbcTemplate jdbcTemplate;

    public LimpiezaTasklet(String tabla, String jobNombre, JdbcTemplate jdbcTemplate) {
        this.tabla = tabla;
        this.jobNombre = jobNombre;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int filas = 0;
        if (tabla != null) {
            filas = jdbcTemplate.update("DELETE FROM " + tabla);
        }
        int errores = jdbcTemplate.update("DELETE FROM errores_batch WHERE job_nombre = ?", jobNombre);

        log.info("Limpieza previa: {} filas eliminadas de {} y {} errores previos de {}.",
                filas, tabla == null ? "(ninguna tabla de detalle)" : tabla, errores, jobNombre);
        return RepeatStatus.FINISHED;
    }
}
