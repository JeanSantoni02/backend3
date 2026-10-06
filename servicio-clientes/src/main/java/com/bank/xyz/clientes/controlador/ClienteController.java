package com.bank.xyz.clientes.controlador;

import com.bank.xyz.clientes.dto.ClienteDto;
import com.bank.xyz.clientes.servicio.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Informacion personal y perfil de los clientes")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los clientes, opcionalmente filtrados por nombre")
    public List<ClienteDto.Respuesta> buscar(@RequestParam(required = false) String nombre) {
        return servicio.buscar(nombre).stream().map(ClienteDto.Respuesta::de).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un cliente")
    public ClienteDto.Respuesta obtener(@PathVariable Long id) {
        return ClienteDto.Respuesta.de(servicio.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un cliente nuevo. El RUT se valida con su digito verificador")
    public ClienteDto.Respuesta alta(@Valid @RequestBody ClienteDto.Alta datos) {
        return ClienteDto.Respuesta.de(servicio.alta(datos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de perfil")
    public ClienteDto.Respuesta perfil(@PathVariable Long id,
                                       @Valid @RequestBody ClienteDto.Perfil datos) {
        return ClienteDto.Respuesta.de(servicio.actualizarPerfil(id, datos));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa o inactiva un cliente")
    public ClienteDto.Respuesta estado(@PathVariable Long id,
                                       @Valid @RequestBody ClienteDto.CambioEstado datos) {
        return ClienteDto.Respuesta.de(servicio.cambiarEstado(id, datos.estado()));
    }
}
