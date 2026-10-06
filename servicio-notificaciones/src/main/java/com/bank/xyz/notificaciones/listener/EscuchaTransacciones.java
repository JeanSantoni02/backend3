package com.bank.xyz.notificaciones.listener;

import com.bank.xyz.notificaciones.modelo.EventoTransaccion;
import com.bank.xyz.notificaciones.modelo.Notificacion;
import com.bank.xyz.notificaciones.repositorio.RepositorioNotificaciones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class EscuchaTransacciones {

    private static final Logger log = LoggerFactory.getLogger(EscuchaTransacciones.class);
    private static final NumberFormat CLP =
            NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

    private final RepositorioNotificaciones repositorio;

    public EscuchaTransacciones(RepositorioNotificaciones repositorio) {
        this.repositorio = repositorio;
    }

    // Suscriptor del topico de eventos de transaccion
    @JmsListener(destination = "${banco.eventos.destino}", containerFactory = "fabricaTopicos")
    public void recibir(EventoTransaccion evento) {
        if (evento == null || evento.getEventoId() == null) {
            log.warn("Evento descartado: llego sin identificador");
            return;
        }

        // El broker puede reentregar un mensaje: sin este control llegarian dos avisos
        if (repositorio.yaProcesado(evento.getEventoId())) {
            log.info("Evento {} ya procesado, se ignora la reentrega", evento.getEventoId());
            return;
        }

        Notificacion n = componer(evento);
        repositorio.guardar(n);

        log.info("Notificacion generada | cuenta={} tipo={} monto={} referencia={}",
                evento.getCuentaId(), evento.getTipo(),
                CLP.format(evento.getMonto()), evento.getReferencia());
    }

    private Notificacion componer(EventoTransaccion e) {
        String titulo = switch (e.getTipo() == null ? "" : e.getTipo()) {
            case "RETIRO" -> "Retiro realizado";
            case "DEPOSITO" -> "Deposito recibido";
            case "TRANSFERENCIA_ENVIADA" -> "Transferencia enviada";
            case "TRANSFERENCIA_RECIBIDA" -> "Transferencia recibida";
            case "PAGO" -> "Pago realizado";
            case "APERTURA" -> "Cuenta abierta";
            case "CIERRE" -> "Cuenta cerrada";
            default -> "Movimiento en tu cuenta";
        };

        String mensaje = switch (e.getTipo() == null ? "" : e.getTipo()) {
            case "APERTURA" -> "Tu cuenta %d ya esta operativa. Saldo inicial: %s."
                    .formatted(e.getCuentaId(), CLP.format(e.getSaldoResultante()));
            case "CIERRE" -> "Tu cuenta %d fue cerrada.".formatted(e.getCuentaId());
            default -> "%s por %s en tu cuenta %d. Saldo disponible: %s."
                    .formatted(titulo, CLP.format(e.getMonto()),
                            e.getCuentaId(), CLP.format(e.getSaldoResultante()));
        };

        return new Notificacion(e.getEventoId(), e.getCuentaId(), titulo, mensaje,
                e.getCanal(), LocalDateTime.now());
    }
}
