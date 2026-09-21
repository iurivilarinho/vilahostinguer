package com.bancada.repository;

import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long>, JpaSpecificationExecutor<Machine> {

    boolean existsByDeviceIdAndContainerNameAndStatusNot(Long deviceId, String containerName, MachineStatus status);

    List<Machine> findByDeviceIdAndStatusNot(Long deviceId, MachineStatus status);

    List<Machine> findByStatus(MachineStatus status);
}
