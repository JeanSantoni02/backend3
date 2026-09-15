package com.bank.xyz.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "banco.batch")
public class BatchProperties {

    /** Filas por transaccion de chunk. */
    private int chunkSize = 50;

    /** Cantidad de particiones (grid size) del step particionado. */
    private int particiones = 4;

    private int hilosCore = 4;
    private int hilosMax = 8;
    private int cola = 16;

    /** Tope de registros que se pueden descartar antes de abortar el step. */
    private int skipLimit = 1000;

    /** Reintentos ante fallas transitorias de infraestructura. */
    private int retryLimit = 3;

    /** Ruta en el classpath donde estan los CSV oficiales. */
    private String rutaDatos = "data/semana_3/";

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getParticiones() { return particiones; }
    public void setParticiones(int particiones) { this.particiones = particiones; }

    public int getHilosCore() { return hilosCore; }
    public void setHilosCore(int hilosCore) { this.hilosCore = hilosCore; }

    public int getHilosMax() { return hilosMax; }
    public void setHilosMax(int hilosMax) { this.hilosMax = hilosMax; }

    public int getCola() { return cola; }
    public void setCola(int cola) { this.cola = cola; }

    public int getSkipLimit() { return skipLimit; }
    public void setSkipLimit(int skipLimit) { this.skipLimit = skipLimit; }

    public int getRetryLimit() { return retryLimit; }
    public void setRetryLimit(int retryLimit) { this.retryLimit = retryLimit; }

    public String getRutaDatos() { return rutaDatos; }
    public void setRutaDatos(String rutaDatos) { this.rutaDatos = rutaDatos; }
}
