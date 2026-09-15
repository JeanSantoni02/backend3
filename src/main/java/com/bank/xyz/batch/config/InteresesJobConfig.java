package com.bank.xyz.batch.config;

import com.bank.xyz.batch.dto.InteresCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.listener.MetricasJobListener;
import com.bank.xyz.batch.listener.RegistroErroresSkipListener;
import com.bank.xyz.batch.model.InteresCalculado;
import com.bank.xyz.batch.partition.RangoLineasPartitioner;
import com.bank.xyz.batch.policy.BancoSkipPolicy;
import com.bank.xyz.batch.processor.InteresProcessor;
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
public class InteresesJobConfig {

    public static final String JOB = "jobCalculoInteresesMensuales";
    private static final String ARCHIVO = "intereses.csv";

    private static final String SQL_LIMPIEZA = "DELETE FROM cuentas";

    private static final String SQL_CONSOLIDAR = """
            INSERT INTO cuentas
                (cuenta_id, nombre, tipo, registros_procesados,
                 interes_total, saldo_final, actualizado_en)
            SELECT cuenta_id,
                   mode() WITHIN GROUP (ORDER BY nombre),
                   mode() WITHIN GROUP (ORDER BY tipo_cuenta),
                   COUNT(*),
                   COALESCE(SUM(interes_mensual), 0),
                   COALESCE(SUM(saldo_final), 0),
                   now()
              FROM intereses_calculados
             GROUP BY cuenta_id
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties propiedades;

    public InteresesJobConfig(JobRepository jobRepository,
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
    public Partitioner particionadorIntereses() {
        return new RangoLineasPartitioner(recurso(), 1);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<InteresCsv> lectorIntereses(
            @Value("#{stepExecutionContext['lineasASaltar']}") Long lineasASaltar,
            @Value("#{stepExecutionContext['cantidadLineas']}") Long cantidadLineas) {

        BeanWrapperFieldSetMapper<InteresCsv> mapeador = new BeanWrapperFieldSetMapper<>();
        mapeador.setTargetType(InteresCsv.class);
        mapeador.setStrict(false);

        return new FlatFileItemReaderBuilder<InteresCsv>()
                .name("lectorIntereses")
                .resource(recurso())
                .encoding("UTF-8")
                .linesToSkip(lineasASaltar == null ? 1 : lineasASaltar.intValue())
                .maxItemCount(cantidadLineas == null ? Integer.MAX_VALUE : cantidadLineas.intValue())
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(mapeador)
                .build();
    }

    @Bean
    public ItemWriter<InteresCalculado> escritorIntereses(EntityManagerFactory emf) {
        JpaItemWriter<InteresCalculado> escritor = new JpaItemWriter<>();
        escritor.setEntityManagerFactory(emf);
        // La clave es autogenerada, asi que persist() evita el SELECT previo
        // que haria merge() en cada una de las 1000 filas.
        escritor.setUsePersist(true);
        return escritor;
    }

    @Bean
    public Step stepCargaInteresesTrabajador(FlatFileItemReader<InteresCsv> lectorIntereses,
                                             InteresProcessor interesProcessor,
                                             ItemWriter<InteresCalculado> escritorIntereses,
                                             RegistroErroresSkipListener registroErrores) {
        return new StepBuilder("stepCargaIntereses", jobRepository)
                .<InteresCsv, InteresCalculado>chunk(propiedades.getChunkSize(), transactionManager)
                .reader(lectorIntereses)
                .processor(interesProcessor)
                .writer(escritorIntereses)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy(propiedades.getSkipLimit()))
                .skip(RegistroInvalidoException.class)
                .retryLimit(propiedades.getRetryLimit())
                .retry(TransientDataAccessException.class)
                .listener(registroErrores)
                .build();
    }

    @Bean
    public Step stepCargaInteresesMaestro(Step stepCargaInteresesTrabajador,
                                          Partitioner particionadorIntereses,
                                          ThreadPoolTaskExecutor ejecutorParticiones) {
        return new StepBuilder("stepCargaInteresesMaestro", jobRepository)
                .partitioner(stepCargaInteresesTrabajador.getName(), particionadorIntereses)
                .step(stepCargaInteresesTrabajador)
                .gridSize(propiedades.getParticiones())
                .taskExecutor(ejecutorParticiones)
                .build();
    }

    @Bean
    public Step stepLimpiarIntereses(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepLimpiarIntereses", jobRepository)
                .tasklet(new LimpiezaTasklet("intereses_calculados", JOB, jdbcTemplate),
                        transactionManager)
                .build();
    }

    @Bean
    public Step stepActualizarCuentas(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepActualizarCuentas", jobRepository)
                .tasklet(new AgregacionTasklet("Consolidacion de saldos por cuenta",
                        SQL_LIMPIEZA, SQL_CONSOLIDAR, jdbcTemplate), transactionManager)
                .build();
    }

    @Bean
    public Job jobCalculoInteresesMensuales(Step stepLimpiarIntereses,
                                            Step stepCargaInteresesMaestro,
                                            Step stepActualizarCuentas,
                                            MetricasJobListener metricas) {
        return new JobBuilder(JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(metricas)
                .start(stepLimpiarIntereses)
                .next(stepCargaInteresesMaestro)
                .next(stepActualizarCuentas)
                .build();
    }
}
