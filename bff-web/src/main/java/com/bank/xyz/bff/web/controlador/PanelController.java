package com.bank.xyz.bff.web.controlador;

import com.bank.xyz.bff.web.cliente.ClienteCoreApi;
import com.bank.xyz.bff.web.cliente.CoreDto;
import com.bank.xyz.bff.web.modelo.PanelCuenta;
import com.bank.xyz.bff.web.servicio.PanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/web")
@Tag(name = "BFF Web", description = "Respuestas completas para el portal de navegador")
public class PanelController {

    private final PanelService panel;
    private final ClienteCoreApi core;

    public PanelController(PanelService panel, ClienteCoreApi core) {
        this.panel = panel;
        this.core = core;
    }

    @GetMapping("/cuentas/{cuentaId}/panel")
    @Operation(summary = "Vista completa de la cuenta en una sola llamada")
    public PanelCuenta panel(@PathVariable Integer cuentaId,
                             @RequestParam(required = false) Integer anio,
                             @RequestParam(defaultValue = "0") int pagina,
                             @RequestParam(defaultValue = "20") int tamano) {
        return panel.panel(cuentaId, anio, pagina, Math.min(tamano, 100));
    }

    @GetMapping("/cuentas")
    @Operation(summary = "Listado paginado de cuentas para la grilla del portal")
    public CoreDto.Pagina<CoreDto.Cuenta> cuentas(@RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamano) {
        return core.cuentas(pagina, Math.min(tamano, 100));
    }
}
