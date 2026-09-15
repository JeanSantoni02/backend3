package com.bank.xyz.bff.atm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "banco.atm")
public class AtmProperties {

    // terminal -> clave. En produccion vendrian de un gestor de secretos, no
    // del archivo de configuracion.
    private Map<String, String> terminales = new HashMap<>();

    private BigDecimal montoMinimo = new BigDecimal("1000");
    private BigDecimal montoMaximo = new BigDecimal("200000");
    private BigDecimal denominacion = new BigDecimal("1000");

    public Map<String, String> getTerminales() { return terminales; }
    public void setTerminales(Map<String, String> terminales) { this.terminales = terminales; }

    public BigDecimal getMontoMinimo() { return montoMinimo; }
    public void setMontoMinimo(BigDecimal montoMinimo) { this.montoMinimo = montoMinimo; }

    public BigDecimal getMontoMaximo() { return montoMaximo; }
    public void setMontoMaximo(BigDecimal montoMaximo) { this.montoMaximo = montoMaximo; }

    public BigDecimal getDenominacion() { return denominacion; }
    public void setDenominacion(BigDecimal denominacion) { this.denominacion = denominacion; }
}
