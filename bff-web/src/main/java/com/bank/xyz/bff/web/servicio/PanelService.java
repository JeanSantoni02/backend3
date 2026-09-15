package com.bank.xyz.bff.web.servicio;

import com.bank.xyz.bff.web.cliente.ClienteCoreApi;
import com.bank.xyz.bff.web.cliente.CoreDto;
import com.bank.xyz.bff.web.modelo.PanelCuenta;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

@Service
public class PanelService {

    private static final String SIN_IDENTIFICAR = "SIN IDENTIFICAR";

    private final ClienteCoreApi core;
    private final ExecutorService ejecutor;

    public PanelService(ClienteCoreApi core, ExecutorService ejecutorBff) {
        this.core = core;
        this.ejecutor = ejecutorBff;
    }

    public PanelCuenta panel(Integer cuentaId, Integer anio, int pagina, int tamano) {
        long inicio = System.currentTimeMillis();

        // Las cuatro consultas son independientes entre si, asi que se lanzan en
        // paralelo. Secuencialmente el portal pagaria la suma de las latencias;
        // en paralelo paga solo la mas lenta.
        var fCuenta = CompletableFuture.supplyAsync(() -> core.cuenta(cuentaId), ejecutor);
        var fEstados = CompletableFuture.supplyAsync(() -> core.estadosAnuales(cuentaId), ejecutor);
        var fIntereses = CompletableFuture.supplyAsync(() -> core.intereses(cuentaId, 0, 100), ejecutor);
        var fMovimientos = CompletableFuture.supplyAsync(
                () -> core.movimientos(cuentaId, anio, pagina, tamano), ejecutor);

        CoreDto.Cuenta cuenta;
        List<CoreDto.EstadoAnual> estados;
        CoreDto.Pagina<CoreDto.Interes> intereses;
        CoreDto.Pagina<CoreDto.Movimiento> movimientos;
        try {
            CompletableFuture.allOf(fCuenta, fEstados, fIntereses, fMovimientos).join();
            cuenta = fCuenta.join();
            estados = fEstados.join();
            intereses = fIntereses.join();
            movimientos = fMovimientos.join();
        } catch (CompletionException e) {
            // join() envuelve lo que haya fallado dentro de la tarea. Sin
            // desenvolverlo, el manejador de errores nunca ve la excepcion real
            // y un servicio de dominio caido se reporta como 500 en vez de 503.
            throw desenvolver(e);
        }

        List<String> avisos = new ArrayList<>();
        boolean identificado = !SIN_IDENTIFICAR.equalsIgnoreCase(cuenta.nombre());
        if (!identificado) {
            avisos.add("El titular de esta cuenta no pudo ser identificado en la migracion.");
        }
        long anomalias = estados.stream()
                .mapToLong(e -> e.movimientosConAnomalia() == null ? 0 : e.movimientosConAnomalia())
                .sum();
        if (anomalias > 0) {
            avisos.add("Hay " + anomalias + " movimiento(s) marcados como anomalia para revision.");
        }
        if (estados.isEmpty()) {
            avisos.add("Esta cuenta no tiene estados de cuenta anuales generados.");
        }

        return new PanelCuenta(
                new PanelCuenta.Titular(cuenta.cuentaId(), cuenta.nombre(), cuenta.tipo(), identificado),
                new PanelCuenta.Resumen(cuenta.saldoFinal(), cuenta.interesTotal(),
                        cuenta.registrosProcesados(), cuenta.actualizadoEn()),
                estados.stream().map(PanelService::aEstadoAnual).toList(),
                resumirIntereses(cuenta.tipo(), intereses.contenido()),
                aPaginaMovimientos(movimientos),
                new PanelCuenta.Metadatos(LocalDateTime.now(),
                        System.currentTimeMillis() - inicio, avisos));
    }

    private static RuntimeException desenvolver(CompletionException e) {
        Throwable causa = e.getCause();
        if (causa instanceof RuntimeException relanzable) {
            return relanzable;
        }
        return e;
    }

    private static PanelCuenta.EstadoAnual aEstadoAnual(CoreDto.EstadoAnual e) {
        return new PanelCuenta.EstadoAnual(e.anio(), e.cantidadMovimientos(), e.totalDepositos(),
                e.totalRetiros(), e.totalCompras(), e.totalPagos(), e.saldoNeto(),
                e.movimientosConAnomalia());
    }

    private static PanelCuenta.ResumenIntereses resumirIntereses(String tipoCuenta,
                                                                 List<CoreDto.Interes> intereses) {
        BigDecimal total = intereses.stream()
                .map(CoreDto.Interes::interesMensual)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tasa = intereses.isEmpty() ? null : intereses.get(0).tasaAnual();
        return new PanelCuenta.ResumenIntereses(tipoCuenta, tasa, intereses.size(), total);
    }

    private static PanelCuenta.PaginaMovimientos aPaginaMovimientos(CoreDto.Pagina<CoreDto.Movimiento> p) {
        List<PanelCuenta.Movimiento> items = p.contenido().stream()
                .map(m -> new PanelCuenta.Movimiento(m.id(), m.fecha(), m.tipo(), m.monto(),
                        m.descripcion(), m.estado(), m.observaciones()))
                .toList();
        return new PanelCuenta.PaginaMovimientos(items, p.pagina(), p.tamano(),
                p.totalElementos(), p.totalPaginas(), p.ultima());
    }
}
