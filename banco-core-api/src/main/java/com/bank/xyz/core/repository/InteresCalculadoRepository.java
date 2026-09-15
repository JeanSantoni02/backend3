package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.InteresCalculado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteresCalculadoRepository extends JpaRepository<InteresCalculado, Long> {

    Page<InteresCalculado> findByCuentaId(Integer cuentaId, Pageable pageable);
}
