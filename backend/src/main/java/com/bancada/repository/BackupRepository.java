package com.bancada.repository;

import com.bancada.enums.BackupKind;
import com.bancada.enums.BackupStatus;
import com.bancada.models.Backup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupRepository extends JpaRepository<Backup, Long>, JpaSpecificationExecutor<Backup> {

    List<Backup> findByStatus(BackupStatus status);

    List<Backup> findByMachineIdAndKindAndStatusInOrderByCreatedAtDesc(Long machineId, BackupKind kind, Collection<BackupStatus> statuses);

    long countByMachineIdAndKindAndStatusIn(Long machineId, BackupKind kind, Collection<BackupStatus> statuses);

    long countByStatus(BackupStatus status);

    Optional<Backup> findFirstByStatusOrderByCreatedAtDesc(BackupStatus status);
}
