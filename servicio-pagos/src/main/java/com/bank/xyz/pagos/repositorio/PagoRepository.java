package com.bank.xyz.pagos.repositorio;

import com.bank.xyz.pagos.modelo.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReferencia(String referencia);

    @Query("select p from Pago p where p.cuentaOrigen = :cuenta or p.cuentaDestino = :cuenta order by p.creadoEn desc")
    List<Pago> deCuenta(@Param("cuenta") Integer cuenta);
}
