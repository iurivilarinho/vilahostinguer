package com.bancada.repository;

import com.bancada.models.Credential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long>, JpaSpecificationExecutor<Credential> {

    Optional<Credential> findFirstByDefaultCredentialTrueAndActiveTrue();

    List<Credential> findByDefaultCredentialTrue();
}
