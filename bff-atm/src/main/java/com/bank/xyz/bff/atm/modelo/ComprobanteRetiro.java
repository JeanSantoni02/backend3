package com.bank.xyz.bff.atm.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComprobanteRetiro(String comprobante,
                                Integer cuenta,
                                BigDecimal montoEntregado,
                                BigDecimal saldoResultante,
                                String terminal,
                                boolean duplicado,
                                LocalDateTime fechaHora) {
}
