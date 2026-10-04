package com.bank.xyz.notificaciones.modelo;

import java.time.LocalDateTime;

public record Notificacion(
        String eventoId,
        Integer cuentaId,
        String titulo,
        String mensaje,
        String canal,
        LocalDateTime recibidoEn) {
}
