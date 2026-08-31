package com.bank.xyz.batch.config;

import com.bank.xyz.batch.model.Cuenta;
import com.bank.xyz.batch.model.CuentaAnual;
import com.bank.xyz.batch.model.Transaccion;
import com.bank.xyz.batch.processor.CuentaAnualProcessor;
import com.bank.xyz.batch.processor.InteresProcessor;
import com.bank.xyz.batch.processor.TransaccionProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
public class BatchConfig {  // 🔽 ELIMINAR @RequiredArgsConstructor

    // 🔽 DECLARAR LAS VARIABLES
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TransaccionProcessor transaccionProcessor;
    private final InteresProcessor interesProcessor;
    private final CuentaAnualProcessor cuentaAnualProcessor;

    // 🔽 AGREGAR EL CONSTRUCTOR MANUALMENTE
    public BatchConfig(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       TransaccionProcessor transaccionProcessor,
                       InteresProcessor interesProcessor,
                       CuentaAnualProcessor cuentaAnualProcessor) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.transaccionProcessor = transaccionProcessor;
        this.interesProcessor = interesProcessor;
        this.cuentaAnualProcessor = cuentaAnualProcessor;
    }

    private static final int CHUNK_SIZE = 5;

    // ================================
    // JOB 1: REPORTE DE TRANSACCIONES
    // ================================
    @Bean
    public FlatFileItemReader<Transaccion> transaccionReader() {
        return new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionReader")
                .resource(new FileSystemResource("src/main/resources/data/semana_3/transacciones.csv"))
                .delimited()
                .names("numeroTransaccion", "fecha", "monto", "tipo")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Transaccion.class);
                }})
                .build();
    }

    @Bean
    public ItemWriter<Transaccion> transaccionWriter() {
        return items -> {
            for (Transaccion t : items) {
                System.out.println("📝 Transacción: " + t.getNumeroTransaccion() + 
                    " | Fecha: " + t.getFecha() + 
                    " | Monto: " + t.getMonto() + 
                    " | Tipo: " + t.getTipo() +
                    (t.getEsAnomalia() ? " ⚠️ ANOMALÍA" : ""));
            }
        };
    }

    @Bean
    public Step stepTransacciones() {
        return new StepBuilder("stepTransacciones", jobRepository)
                .<Transaccion, Transaccion>chunk(CHUNK_SIZE, transactionManager)
                .reader(transaccionReader())
                .processor(transaccionProcessor)
                .writer(transaccionWriter())
                .build();
    }

    @Bean
    public Job jobReporteTransacciones() {
        return new JobBuilder("jobReporteTransacciones", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepTransacciones())
                .build();
    }

    // ================================
    // JOB 2: CÁLCULO DE INTERESES
    // ================================
    @Bean
    public FlatFileItemReader<Cuenta> interesReader() {
        return new FlatFileItemReaderBuilder<Cuenta>()
                .name("interesReader")
                .resource(new FileSystemResource("src/main/resources/data/semana_3/intereses.csv"))
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Cuenta.class);
                }})
                .build();
    }

    @Bean
    public ItemWriter<Cuenta> interesWriter() {
        return items -> {
            for (Cuenta c : items) {
                System.out.println("💰 Cuenta: " + c.getCuentaId() + 
                    " | Nombre: " + c.getNombre() + 
                    " | Saldo: " + c.getSaldo() + 
                    " | Tipo: " + c.getTipo());
            }
        };
    }

    @Bean
    public Step stepIntereses() {
        return new StepBuilder("stepIntereses", jobRepository)
                .<Cuenta, Cuenta>chunk(CHUNK_SIZE, transactionManager)
                .reader(interesReader())
                .processor(interesProcessor)
                .writer(interesWriter())
                .build();
    }

    @Bean
    public Job jobIntereses() {
        return new JobBuilder("jobIntereses", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepIntereses())
                .build();
    }

    // ================================
    // JOB 3: ESTADOS DE CUENTA ANUALES
    // ================================
    @Bean
    public FlatFileItemReader<CuentaAnual> cuentaAnualReader() {
        return new FlatFileItemReaderBuilder<CuentaAnual>()
                .name("cuentaAnualReader")
                .resource(new FileSystemResource("src/main/resources/data/semana_3/cuentas_anuales.csv"))
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CuentaAnual.class);
                }})
                .build();
    }

    @Bean
    public ItemWriter<CuentaAnual> cuentaAnualWriter() {
        return items -> {
            for (CuentaAnual c : items) {
                System.out.println("📊 Cuenta Anual: " + c.getCuentaId() + 
                    " | Fecha: " + c.getFecha() + 
                    " | Transacción: " + c.getTransaccion() + 
                    " | Monto: " + c.getMonto());
            }
        };
    }

    @Bean
    public Step stepCuentaAnual() {
        return new StepBuilder("stepCuentaAnual", jobRepository)
                .<CuentaAnual, CuentaAnual>chunk(CHUNK_SIZE, transactionManager)
                .reader(cuentaAnualReader())
                .processor(cuentaAnualProcessor)
                .writer(cuentaAnualWriter())
                .build();
    }

    @Bean
    public Job jobCuentaAnual() {
        return new JobBuilder("jobCuentaAnual", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stepCuentaAnual())
                .build();
    }
}