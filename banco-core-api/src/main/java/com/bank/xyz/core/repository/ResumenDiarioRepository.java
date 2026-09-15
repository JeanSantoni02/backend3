package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.ResumenDiario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ResumenDiarioRepository extends JpaRepository<ResumenDiario, Long> {

    Page<ResumenDiario> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta, Pageable pageable);

    Page<ResumenDiario> findAllByOrderByFechaDesc(Pageable pageable);
}
