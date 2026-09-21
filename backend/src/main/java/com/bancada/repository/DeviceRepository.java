package com.bancada.repository;

import com.bancada.enums.DeviceStatus;
import com.bancada.models.Device;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByHostKeyFingerprint(String hostKeyFingerprint);

    List<Device> findByActiveTrue();

    long countByActiveTrue();

    long countByActiveTrueAndOnlineTrue();

    long countByActiveTrueAndStatusIn(Collection<DeviceStatus> statuses);
}
