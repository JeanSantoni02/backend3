package com.bank.xyz.core.repository;

import com.bank.xyz.core.model.OperacionAtm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacionAtmRepository extends JpaRepository<OperacionAtm, Long> {

    Optional<OperacionAtm> findByReferencia(String referencia);
}
