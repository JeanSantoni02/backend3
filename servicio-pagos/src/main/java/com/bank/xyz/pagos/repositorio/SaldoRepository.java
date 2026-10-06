package com.bank.xyz.pagos.repositorio;

import com.bank.xyz.pagos.modelo.SaldoCuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SaldoRepository extends JpaRepository<SaldoCuenta, Integer> {

    // SELECT ... FOR UPDATE: dos operaciones sobre la misma cuenta se ordenan en fila
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SaldoCuenta s where s.cuentaId = :id")
    Optional<SaldoCuenta> bloquear(@Param("id") Integer id);
}
