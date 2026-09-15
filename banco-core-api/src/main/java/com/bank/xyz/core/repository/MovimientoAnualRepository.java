package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.MovimientoAnual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoAnualRepository extends JpaRepository<MovimientoAnual, Long> {

    Page<MovimientoAnual> findByCuentaIdOrderByFechaDesc(Integer cuentaId, Pageable pageable);

    Page<MovimientoAnual> findByCuentaIdAndAnioOrderByFechaDesc(Integer cuentaId, Integer anio, Pageable pageable);

    List<MovimientoAnual> findTop10ByCuentaIdOrderByFechaDescIdDesc(Integer cuentaId);
}
