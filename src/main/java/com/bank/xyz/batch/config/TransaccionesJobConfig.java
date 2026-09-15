package com.bank.xyz.batch.config;

import com.bank.xyz.batch.dto.TransaccionCsv;
import com.bank.xyz.batch.exception.RegistroInvalidoException;
import com.bank.xyz.batch.listener.MetricasJobListener;
import com.bank.xyz.batch.listener.RegistroErroresSkipListener;
import com.bank.xyz.batch.model.Transaccion;
import com.bank.xyz.batch.partition.RangoLineasPartitioner;
import com.bank.xyz.batch.policy.BancoSkipPolicy;
import com.bank.xyz.batch.processor.TransaccionProcessor;
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
public class TransaccionesJobConfig {

    public static final String JOB = "jobReporteTransaccionesDiarias";
    private static final String ARCHIVO = "transacciones.csv";

    private static final String SQL_LIMPIEZA = "DELETE FROM resumen_diario";

    private static final String SQL_RESUMEN = """
            INSERT INTO resumen_diario
                (fecha, total_transacciones, monto_total, monto_promedio,
                 monto_maximo, cantidad_anomalias, generado_en)
            SELECT fecha,
                   COUNT(*),
                   COALESCE(SUM(monto), 0),
                   COALESCE(ROUND(AVG(monto), 2), 0),
                   COALESCE(MAX(monto), 0),
                   COUNT(*) FILTER (WHERE estado = 'ANOMALIA'),
                   now()
              FROM transacciones
             WHERE fecha IS NOT NULL
             GROUP BY fecha
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final BatchProperties propiedades;

    public TransaccionesJobConfig(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  BatchProperties propiedades) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.propiedades = propiedades;
    }

    private Resource recurso() {
        return new ClassPathResource(propiedades.getRutaDatos() + ARCHIVO);
    }

    // ------------------------------------------------------------------
    //  Particionador
    // ------------------------------------------------------------------
    @Bean
    public Partitioner particionadorTransacciones() {
        return new RangoLineasPartitioner(recurso(), 1);
    }

    // ------------------------------------------------------------------
    //  Reader: uno por particion, por eso @StepScope.
    //  Los valores llegan del ExecutionContext que arma el particionador.
    // ------------------------------------------------------------------
    @Bean
    @StepScope
    public FlatFileItemReader<TransaccionCsv> lectorTransacciones(
            @Value("#{stepExecutionContext['lineasASaltar']}") Long lineasASaltar,
            @Value("#{stepExecutionContext['cantidadLineas']}") Long cantidadLineas) {

        BeanWrapperFieldSetMapper<TransaccionCsv> mapeador = new BeanWrapperFieldSetMapper<>();
        mapeador.setTargetType(TransaccionCsv.class);
        mapeador.setStrict(false);

        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("lectorTransacciones")
                .resource(recurso())
                .encoding("UTF-8")
                .linesToSkip(lineasASaltar == null ? 1 : lineasASaltar.intValue())
                .maxItemCount(cantidadLineas == null ? Integer.MAX_VALUE : cantidadLineas.intValue())
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(mapeador)
                .build();
    }

    @Bean
    public ItemWriter<Transaccion> escritorTransacciones(EntityManagerFactory emf) {
        JpaItemWriter<Transaccion> escritor = new JpaItemWriter<>();
        escritor.setEntityManagerFactory(emf);
        // merge (no persist) porque el id viene del archivo: reprocesar el mismo
        // archivo actualiza la fila en vez de duplicarla.
        return escritor;
    }

    // ------------------------------------------------------------------
    //  Step trabajador: el que corre en paralelo, uno por particion
    // ------------------------------------------------------------------
    @Bean
    public Step stepCargaTransaccionesTrabajador(
            FlatFileItemReader<TransaccionCsv> lectorTransacciones,
            TransaccionProcessor transaccionProcessor,
            ItemWriter<Transaccion> escritorTransacciones,
            RegistroErroresSkipListener registroErrores) {

        return new StepBuilder("stepCargaTransacciones", jobRepository)
                .<TransaccionCsv, Transaccion>chunk(propiedades.getChunkSize(), transactionManager)
                .reader(lectorTransacciones)
                .processor(transaccionProcessor)
                .writer(escritorTransacciones)
                .faultTolerant()
                .skipPolicy(new BancoSkipPolicy(propiedades.getSkipLimit()))
                .skip(RegistroInvalidoException.class)
                // Fallas transitorias de base de datos: se reintenta el chunk en
                // vez de perder registros validos.
                .retryLimit(propiedades.getRetryLimit())
                .retry(TransientDataAccessException.class)
                .listener(registroErrores)
                .build();
    }

    // ------------------------------------------------------------------
    //  Step maestro: reparte las particiones entre los hilos del pool
    // ------------------------------------------------------------------
    @Bean
    public Step stepCargaTransaccionesMaestro(Step stepCargaTransaccionesTrabajador,
                                              Partitioner particionadorTransacciones,
                                              ThreadPoolTaskExecutor ejecutorParticiones) {
        return new StepBuilder("stepCargaTransaccionesMaestro", jobRepository)
                .partitioner(stepCargaTransaccionesTrabajador.getName(), particionadorTransacciones)
                .step(stepCargaTransaccionesTrabajador)
                .gridSize(propiedades.getParticiones())
                .taskExecutor(ejecutorParticiones)
                .build();
    }

    // ------------------------------------------------------------------
    //  Step de resumen
    // ------------------------------------------------------------------
    @Bean
    public Step stepLimpiarErroresTransacciones(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepLimpiarErroresTransacciones", jobRepository)
                .tasklet(new LimpiezaTasklet(null, JOB, jdbcTemplate), transactionManager)
                .build();
    }

    @Bean
    public Step stepResumenDiario(JdbcTemplate jdbcTemplate) {
        return new StepBuilder("stepResumenDiario", jobRepository)
                .tasklet(new AgregacionTasklet("Resumen diario de transacciones",
                        SQL_LIMPIEZA, SQL_RESUMEN, jdbcTemplate), transactionManager)
                .build();
    }

    @Bean
    public Job jobReporteTransaccionesDiarias(Step stepLimpiarErroresTransacciones,
                                              Step stepCargaTransaccionesMaestro,
                                              Step stepResumenDiario,
                                              MetricasJobListener metricas) {
        return new JobBuilder(JOB, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(metricas)
                .start(stepLimpiarErroresTransacciones)
                .next(stepCargaTransaccionesMaestro)
                .next(stepResumenDiario)
                .build();
    }
}
