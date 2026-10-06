package com.bank.xyz.clientes.dto;

import com.bank.xyz.clientes.modelo.Cliente;
import com.bank.xyz.clientes.validacion.RutValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ClienteDto {

    private ClienteDto() {
    }

    public record Alta(
            @NotBlank @RutValido String rut,
            @NotBlank @Size(max = 100) String nombre,
            @Email @Size(max = 120) String email,
            @Pattern(regexp = "\\+?\\d{8,15}", message = "telefono invalido") String telefono,
            @Size(max = 160) String direccion,
            @Past LocalDate fechaNacimiento,
            Cliente.Segmento segmento) {
    }

    // El RUT no se modifica: identifica al cliente
    public record Perfil(
            @NotBlank @Size(max = 100) String nombre,
            @Email @Size(max = 120) String email,
            @Pattern(regexp = "\\+?\\d{8,15}", message = "telefono invalido") String telefono,
            @Size(max = 160) String direccion,
            @Past LocalDate fechaNacimiento,
            Cliente.Segmento segmento) {
    }

    public record CambioEstado(@NotNull Cliente.EstadoCliente estado) {
    }

    public record Respuesta(
            Long id, String rut, String nombre, String email, String telefono,
            String direccion, LocalDate fechaNacimiento, Cliente.Segmento segmento,
            Cliente.EstadoCliente estado, LocalDateTime creadoEn, LocalDateTime actualizadoEn) {

        public static Respuesta de(Cliente c) {
            return new Respuesta(c.getId(), c.getRut(), c.getNombre(), c.getEmail(),
                    c.getTelefono(), c.getDireccion(), c.getFechaNacimiento(), c.getSegmento(),
                    c.getEstado(), c.getCreadoEn(), c.getActualizadoEn());
        }
    }
}
