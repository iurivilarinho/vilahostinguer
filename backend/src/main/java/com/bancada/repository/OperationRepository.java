package com.bancada.repository;

import com.bancada.enums.OperationStatus;
import com.bancada.models.Operation;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long>, JpaSpecificationExecutor<Operation> {

    long countByStatusIn(Collection<OperationStatus> statuses);

    long countByStatusAndFinishedAtAfter(OperationStatus status, LocalDateTime after);

    List<Operation> findByStatusIn(Collection<OperationStatus> statuses);
}
