package com.bank.xyz.batch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

public class AgregacionTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(AgregacionTasklet.class);

    private final String descripcion;
    private final String sqlLimpieza;
    private final String sqlAgregacion;
    private final JdbcTemplate jdbcTemplate;

    public AgregacionTasklet(String descripcion, String sqlLimpieza, String sqlAgregacion,
                             JdbcTemplate jdbcTemplate) {
        this.descripcion = descripcion;
        this.sqlLimpieza = sqlLimpieza;
        this.sqlAgregacion = sqlAgregacion;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int borradas = jdbcTemplate.update(sqlLimpieza);
        int generadas = jdbcTemplate.update(sqlAgregacion);

        log.info("{}: se eliminaron {} filas previas y se generaron {} filas de resumen.",
                descripcion, borradas, generadas);

        contribution.incrementWriteCount(generadas);
        return RepeatStatus.FINISHED;
    }
}
