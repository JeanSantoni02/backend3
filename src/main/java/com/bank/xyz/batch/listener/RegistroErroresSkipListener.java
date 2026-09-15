package com.bank.xyz.batch.listener;

import com.bank.xyz.batch.exception.RegistroInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;


@Component
public class RegistroErroresSkipListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(RegistroErroresSkipListener.class);

    private static final String INSERT = """
            INSERT INTO errores_batch
                (job_nombre, step_nombre, fase, motivo, registro, excepcion, ocurrido_en)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaccionIndependiente;

    public RegistroErroresSkipListener(JdbcTemplate jdbcTemplate,
                                       PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transaccionIndependiente = new TransactionTemplate(transactionManager);
        this.transaccionIndependiente.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void onSkipInRead(Throwable t) {
        registrar("LECTURA", null, t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        registrar("PROCESO", item, t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        registrar("ESCRITURA", item, t);
    }

    private void registrar(String fase, Object item, Throwable t) {
        String jobNombre = "desconocido";
        String stepNombre = "desconocido";
        StepContext contexto = StepSynchronizationManager.getContext();
        if (contexto != null) {
            jobNombre = contexto.getStepExecution().getJobExecution().getJobInstance().getJobName();
            stepNombre = contexto.getStepExecution().getStepName();
        }

        String motivo = (t instanceof RegistroInvalidoException invalido)
                ? invalido.getMotivo()
                : String.valueOf(t.getMessage());

        String registro = (item != null)
                ? String.valueOf(item)
                : (t instanceof RegistroInvalidoException invalido ? invalido.getDatoOriginal() : null);

        final String fJob = jobNombre;
        final String fStep = stepNombre;
        try {
            transaccionIndependiente.executeWithoutResult(estado -> jdbcTemplate.update(INSERT,
                    fJob,
                    fStep,
                    fase,
                    recortar(motivo, 300),
                    recortar(registro, 500),
                    recortar(t.getClass().getSimpleName(), 200),
                    Timestamp.valueOf(LocalDateTime.now())));
        } catch (Exception e) {
            // Nunca hacer fallar el job por no poder anotar el error.
            log.error("No se pudo registrar el error en errores_batch: {}", e.getMessage());
        }
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
