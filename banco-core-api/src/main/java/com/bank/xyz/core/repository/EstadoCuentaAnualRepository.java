package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.EstadoCuentaAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCuentaAnualRepository extends JpaRepository<EstadoCuentaAnual, Long> {

    List<EstadoCuentaAnual> findByCuentaIdOrderByAnioDesc(Integer cuentaId);

    Optional<EstadoCuentaAnual> findByCuentaIdAndAnio(Integer cuentaId, Integer anio);
}
