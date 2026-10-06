package com.bank.xyz.clientes.repositorio;

import com.bank.xyz.clientes.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRut(String rut);

    boolean existsByRut(String rut);

    List<Cliente> findByNombreContainingIgnoreCaseOrderByNombre(String nombre);
}
