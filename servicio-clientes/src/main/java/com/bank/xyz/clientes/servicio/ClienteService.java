package com.bank.xyz.clientes.servicio;

import com.bank.xyz.clientes.dto.ClienteDto;
import com.bank.xyz.clientes.error.ManejadorErrores.ConflictoException;
import com.bank.xyz.clientes.error.ManejadorErrores.NoEncontradoException;
import com.bank.xyz.clientes.modelo.Cliente;
import com.bank.xyz.clientes.repositorio.ClienteRepository;
import com.bank.xyz.clientes.validacion.Rut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final ClienteRepository repositorio;

    public ClienteService(ClienteRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return repositorio.findAll();
        }
        return repositorio.findByNombreContainingIgnoreCaseOrderByNombre(nombre.trim());
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new NoEncontradoException("no existe el cliente " + id));
    }

    @Transactional
    public Cliente alta(ClienteDto.Alta datos) {
        String rut = Rut.normalizar(datos.rut());
        if (repositorio.existsByRut(rut)) {
            throw new ConflictoException("ya existe un cliente con el RUT " + rut);
        }

        Cliente c = new Cliente();
        c.setRut(rut);
        aplicar(c, datos.nombre(), datos.email(), datos.telefono(), datos.direccion(),
                datos.fechaNacimiento(), datos.segmento());

        Cliente guardado = repositorio.save(c);
        log.info("Cliente creado | id={} rut={}", guardado.getId(), guardado.getRut());
        return guardado;
    }

    @Transactional
    public Cliente actualizarPerfil(Long id, ClienteDto.Perfil datos) {
        Cliente c = obtener(id);
        aplicar(c, datos.nombre(), datos.email(), datos.telefono(), datos.direccion(),
                datos.fechaNacimiento(), datos.segmento());
        log.info("Perfil actualizado | id={}", id);
        return c;
    }

    @Transactional
    public Cliente cambiarEstado(Long id, Cliente.EstadoCliente estado) {
        Cliente c = obtener(id);
        c.setEstado(estado);
        log.info("Estado de cliente | id={} estado={}", id, estado);
        return c;
    }

    private void aplicar(Cliente c, String nombre, String email, String telefono,
                         String direccion, java.time.LocalDate nacimiento, Cliente.Segmento segmento) {
        c.setNombre(nombre.trim());
        c.setEmail(email);
        c.setTelefono(telefono);
        c.setDireccion(direccion);
        c.setFechaNacimiento(nacimiento);
        if (segmento != null) {
            c.setSegmento(segmento);
        }
    }
}
