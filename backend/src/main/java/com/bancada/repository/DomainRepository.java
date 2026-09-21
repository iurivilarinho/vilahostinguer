package com.bancada.repository;

import com.bancada.models.Domain;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long>, JpaSpecificationExecutor<Domain> {

    boolean existsByNameAndActiveTrueAndIdNot(String name, Long id);

    List<Domain> findByActiveTrue();
}
