package com.bank.xyz.batch.config;

import com.bank.xyz.batch.dto.MovimientoAnualCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.listener.MetricasJobListener;
import com.bank.xyz.batch.listener.RegistroErroresSkipListener;
import com.bank.xyz.batch.model.MovimientoAnual;
import com.bank.xyz.batch.partition.RangoLineasPartitioner;
import com.bank.xyz.batch.policy.BancoSkipPolicy;
import com.bank.xyz.batch.processor.MovimientoAnualProcessor;
import com.bank.xyz.batch.tasklet.AgregacionTasklet;
import com.bank.xyz.batch.tasklet.LimpiezaTasklet;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
public class EstadoCuentaAnualJobConfig {

    public static final String JOB = "jobEstadosCuentaAnuales";
    private static final String ARCHIVO = "cuentas_anuales.csv";

    private static final String SQL_LIMPIEZA = "DELETE FROM estado_cuenta_anual";

    private static final String SQL_ESTADO = """
            INSERT INTO estado_cuenta_anual
                (cuenta_id, anio, cantidad_movimientos, total_depositos, total_retiros,
                 total_compras, total_pagos, saldo_neto, movimientos_con_anomalia, generado_en)
            SELECT cuenta_id,
                   anio,
                   COUNT(*),
                   COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'deposito'), 0),
                   COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'retiro'), 0),
                   COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'compra'), 0),
                   COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'pago'), 0),
                   COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento = 'deposito'), 0)
                     - COALESCE(SUM(monto) FILTER (WHERE tipo_movimiento IN ('retiro', 'compra', 'pago')), 0),
                   COUNT(*) FILTER (WHERE estado = 'ANOMALIA'),
                   now()
              FROM movimientos_anuales
             GROUP BY cuenta_id, anio
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties propiedades;

    public EstadoCuentaAnualJobConfig(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      BatchProperties propiedades) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.propiedades = propiedades;
    }

    private Resource recurso() {
        return new ClassPathResource(propiedades.getRutaDatos() + ARCHIVO);
    }

    @Bean
    public Partitioner particionadorMovimientos() {
        return new RangoLineasPartitioner(recurso(), 1);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<MovimientoAnualCsv> lectorMovimientos(
            @Value("#{stepExecutionContext['lineasASaltar']}") Long lineasASaltar,
            @Value("#{stepExecutionContext['cantidadLineas']}") Long cantidadLineas) {

        BeanWrapperFieldSetMapper<MovimientoAnualCsv> mapeador = new BeanWrapperFieldSetMapper<>();
        mapeador.setTargetType(MovimientoAnualCsv.class);
        mapeador.setStrict(false);

        return new FlatFileItemReaderBuilder<MovimientoAnualCsv>()
                .name("lectorMovimientos")
                .resource(recurso())
                .encoding("UTF-8")
                .linesToSkip(lineasASaltar == null ? 1 : lineasASaltar.intValue())
                .maxItemCount(cantidadLineas == null ? Integer.MAX_VALUE : cantidadLineas.intValue())
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(mapeador)
                .build();
    }

    @Bean
    public ItemWriter<MovimientoAnual> escritorMovimientos(EntityManagerFactory emf) {
        JpaItemWriter<MovimientoAnual> escritor = new JpaItemWriter<>();
        escritor.setEntityManagerFactory(emf);
        escritor.setUsePersist(true);
        return escritor;
    }

    @Bean
    public Step stepCargaMovimientosTrabajador(FlatFileItemReader<MovimientoAnualCsv> lectorMovimientos,
                                               MovimientoAnualProcessor movimientoAnualProcessor,
                                               ItemWriter<MovimientoAnual> escritorMovimientos,
                                               RegistroErroresSkipListener registroErrores) {
        return new StepBuilder("stepCargaMovimientos", jobRepository)
                .<MovimientoAnualCsv, MovimientoAnual>chunk(propiedades.getChunkSize(), transactionManager)
                .reader(lectorMovimientos)
                .processor(movimientoAnualProcessor)
                .writer(escritorMovimientos)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy(propiedades.getSkipLimit()))
                .skip(RegistroInvalidoException.class)
                .retryLimit(propiedades.getRetryLimit())
                .retry(TransientDataAccessException.class)
                .listener(registroErrores)
                .build();
    }

    @Bean
    public Step stepCargaMovimientosMaestro(Step stepCargaMovimientosTrabajador,
                                            Partitioner particionadorMovimientos,
                                            ThreadPoolTaskExecutor ejecutorParticiones) {
        return new StepBuilder("stepCargaMovimientosMaestro", jobRepository)
                .partitioner(stepCargaMovimientosTrabajador.getName(), particionadorMovimientos)
                .step(stepCargaMovimientosTrabajador)
                .gridSize(propiedades.getParticiones())
                .taskExecutor(ejecutorParticiones)
                .build();
    }

    @Bean
    public Step stepLimpiarMovimientos(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepLimpiarMovimientos", jobRepository)
                .tasklet(new LimpiezaTasklet("movimientos_anuales", JOB, jdbcTemplate),
                        transactionManager)
                .build();
    }

    @Bean
    public Step stepEstadoCuentaAnual(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepEstadoCuentaAnual", jobRepository)
                .tasklet(new AgregacionTasklet("Estados de cuenta anuales",
                        SQL_LIMPIEZA, SQL_ESTADO, jdbcTemplate), transactionManager)
                .build();
    }

    @Bean
    public Job jobEstadosCuentaAnuales(Step stepLimpiarMovimientos,
                                       Step stepCargaMovimientosMaestro,
                                       Step stepEstadoCuentaAnual,
                                       MetricasJobListener metricas) {
        return new JobBuilder(JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(metricas)
                .start(stepLimpiarMovimientos)
                .next(stepCargaMovimientosMaestro)
                .next(stepEstadoCuentaAnual)
                .build();
    }
}
