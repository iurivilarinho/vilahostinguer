package com.bancada.repository;

import com.bancada.enums.VolumeStatus;
import com.bancada.models.Volume;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VolumeRepository extends JpaRepository<Volume, Long>, JpaSpecificationExecutor<Volume> {

    List<Volume> findByStatusNot(VolumeStatus status);

    List<Volume> findByStatusIn(Collection<VolumeStatus> statuses);

    List<Volume> findByMachineId(Long machineId);

    Optional<Volume> findByExportNameAndStatusNot(String exportName, VolumeStatus status);

    boolean existsByNameAndStatusNot(String name, VolumeStatus status);

    boolean existsByDeviceIdAndMountPathAndIdNot(Long deviceId, String mountPath, Long id);
}
