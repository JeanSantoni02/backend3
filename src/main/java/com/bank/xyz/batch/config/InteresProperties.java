package com.bank.xyz.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "banco.interes")
public class InteresProperties {

    private BigDecimal ahorro = new BigDecimal("2.5");
    private BigDecimal prestamo = new BigDecimal("12.0");
    private BigDecimal hipoteca = new BigDecimal("8.0");

    /** @return la tasa anual del tipo, o null si el tipo no es reconocido. */
    public BigDecimal tasaDe(String tipoNormalizado) {
        if (tipoNormalizado == null) {
            return null;
        }
        return switch (tipoNormalizado) {
            case "ahorro" -> ahorro;
            case "prestamo" -> prestamo;
            case "hipoteca" -> hipoteca;
            default -> null;
        };
    }

    public BigDecimal getAhorro() { return ahorro; }
    public void setAhorro(BigDecimal ahorro) { this.ahorro = ahorro; }

    public BigDecimal getPrestamo() { return prestamo; }
    public void setPrestamo(BigDecimal prestamo) { this.prestamo = prestamo; }

    public BigDecimal getHipoteca() { return hipoteca; }
    public void setHipoteca(BigDecimal hipoteca) { this.hipoteca = hipoteca; }
}
