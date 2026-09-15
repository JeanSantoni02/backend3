package com.bank.xyz.batch.policy;

import com.bank.xyz.batch.exception.RegistroInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ParseException;
import org.springframework.dao.DataAccessException;


public class BancoSkipPolicy implements SkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(BancoSkipPolicy.class);

    /** Tope de lineas malformadas antes de asumir que el archivo esta corrupto. */
    private static final int TOPE_ARCHIVO_CORRUPTO = 50;

    private final int topeRegistrosInvalidos;

    public BancoSkipPolicy(int topeRegistrosInvalidos) {
        this.topeRegistrosInvalidos = topeRegistrosInvalidos;
    }

    @Override
    public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
        if (t instanceof RegistroInvalidoException) {
            if (skipCount >= topeRegistrosInvalidos) {
                log.error("Se supero el tope de {} registros invalidos. Se aborta el step.",
                        topeRegistrosInvalidos);
                return false;
            }
            return true;
        }

        if (t instanceof ParseException) {
            if (skipCount >= TOPE_ARCHIVO_CORRUPTO) {
                log.error("Mas de {} lineas malformadas: el archivo de entrada parece corrupto. Se aborta.",
                        TOPE_ARCHIVO_CORRUPTO);
                return false;
            }
            log.warn("Linea malformada descartada: {}", t.getMessage());
            return true;
        }

        if (t instanceof DataAccessException) {
            // Error de infraestructura: lo resuelve el retry, no el skip.
            log.error("Error de acceso a datos, no se descarta el registro: {}", t.getMessage());
            return false;
        }

        return false;
    }
}
