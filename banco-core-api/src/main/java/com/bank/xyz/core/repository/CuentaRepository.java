package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    // Bloqueo pesimista: si dos cajeros retiran de la misma cuenta a la vez,
    // el segundo espera al primero en vez de leer un saldo ya obsoleto.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.cuentaId = :id")
    Optional<Cuenta> buscarParaActualizar(@Param("id") Integer id);
}
