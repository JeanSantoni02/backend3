package com.bank.xyz.cuentas.repositorio;

import com.bank.xyz.cuentas.modelo.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    List<Cuenta> findByClienteIdOrderByCuentaId(Long clienteId);

    // Bloquea la fila para que un pago concurrente no cambie el saldo durante el cierre
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.cuentaId = :id")
    Optional<Cuenta> bloquear(@Param("id") Integer id);
}
