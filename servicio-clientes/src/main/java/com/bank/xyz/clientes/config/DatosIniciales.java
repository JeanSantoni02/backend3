package com.bank.xyz.clientes.config;

import com.bank.xyz.clientes.modelo.Cliente;
import com.bank.xyz.clientes.repositorio.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// Titulares del archivo legacy de intereses, para abrir cuentas a clientes reales
@Component
public class DatosIniciales implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ClienteRepository repositorio;

    public DatosIniciales(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.count() > 0) {
            return;
        }

        List<Cliente> semilla = List.of(
                cliente("12345678-5", "Diana Prince", "diana.prince@correo.cl", LocalDate.of(1985, 3, 21)),
                cliente("11111111-1", "John Doe", "john.doe@correo.cl", LocalDate.of(1979, 7, 2)),
                cliente("15234567-4", "Bob Johnson", "bob.johnson@correo.cl", LocalDate.of(1990, 11, 9)),
                cliente("16789012-1", "Alice Smith", "alice.smith@correo.cl", LocalDate.of(1993, 1, 15)),
                cliente("9876543-3", "Charlie Brown", "charlie.brown@correo.cl", LocalDate.of(1968, 5, 30)));

        repositorio.saveAll(semilla);
        log.info("Clientes iniciales cargados: {}", semilla.size());
    }

    private Cliente cliente(String rut, String nombre, String email, LocalDate nacimiento) {
        Cliente c = new Cliente();
        c.setRut(rut);
        c.setNombre(nombre);
        c.setEmail(email);
        c.setFechaNacimiento(nacimiento);
        return c;
    }
}
