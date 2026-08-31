package com.bank.xyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Component
public class CustomSkipListener<T, S> implements SkipListener<T, S> {

    // 🔽 AGREGAR ESTA LÍNEA 🔽
    private static final Logger log = LoggerFactory.getLogger(CustomSkipListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("❌ Error en LECTURA (SKIP): {}", t.getMessage());
    }

    @Override
    public void onSkipInWrite(S item, Throwable t) {
        log.error("❌ Error en ESCRITURA (SKIP) para item: {} - {}", item, t.getMessage());
    }

    @Override
    public void onSkipInProcess(T item, Throwable t) {
        log.error("❌ Error en PROCESAMIENTO (SKIP) para item: {} - {}", item, t.getMessage());
    }
}