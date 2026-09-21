package com.bancada.service;

import com.bancada.enums.BackupStatus;
import com.bancada.enums.DeviceStatus;
import com.bancada.enums.OperationStatus;
import com.bancada.models.Backup;
import com.bancada.repository.BackupRepository;
import com.bancada.repository.DeviceRepository;
import com.bancada.repository.OperationRepository;
import com.bancada.response.DashboardSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final OperationRepository operationRepository;
    private final BackupRepository backupRepository;

    public DashboardService(DeviceRepository deviceRepository, OperationRepository operationRepository,
                            BackupRepository backupRepository) {
        this.deviceRepository = deviceRepository;
        this.operationRepository = operationRepository;
        this.backupRepository = backupRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        LocalDateTime lastBackupAt = backupRepository.findFirstByStatusOrderByCreatedAtDesc(BackupStatus.AVAILABLE)
            .map(Backup::getCreatedAt)
            .orElse(null);
        return new DashboardSummaryResponse(
            deviceRepository.countByActiveTrue(),
            deviceRepository.countByActiveTrueAndOnlineTrue(),
            deviceRepository.countByActiveTrueAndStatusIn(List.of(DeviceStatus.DISCOVERED, DeviceStatus.AUTH_FAILED)),
            operationRepository.countByStatusIn(List.of(OperationStatus.PENDING, OperationStatus.RUNNING)),
            operationRepository.countByStatusAndFinishedAtAfter(OperationStatus.FAILED, LocalDateTime.now().minusDays(1)),
            backupRepository.countByStatus(BackupStatus.AVAILABLE),
            lastBackupAt);
    }
}
