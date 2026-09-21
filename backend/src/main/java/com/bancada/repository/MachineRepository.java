package com.bancada.repository;

import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long>, JpaSpecificationExecutor<Machine> {

    boolean existsByNameAndStatusNot(String name, MachineStatus status);

    List<Machine> findByStatusNot(MachineStatus status);

    Optional<Machine> findByDeviceId(Long deviceId);

    List<Machine> findByStatus(MachineStatus status);
}
