package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import com.bancada.records.HyperVStatus;
import com.bancada.repository.MachineRepository;
import com.bancada.request.MachineRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    private static final HyperVStatus READY = new HyperVStatus(true, true, true, 16_384, 12, null, "Bancada", "10.77.0.0/24");

    @Mock
    private MachineRepository machineRepository;
    @Mock
    private HyperVService hyperVService;
    @Mock
    private MachineImageService machineImageService;
    @Mock
    private HostDiskService hostDiskService;
    @Mock
    private DeviceService deviceService;
    @Mock
    private CredentialService credentialService;
    @Mock
    private SshService sshService;
    @Mock
    private OperationService operationService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private SecretCipherService secretCipherService;

    private MachineService machineService;

    @BeforeEach
    void setUp() {
        machineService = new MachineService(machineRepository, hyperVService, machineImageService, hostDiskService, deviceService,
            credentialService, sshService, operationService, applicationEventPublisher, secretCipherService, 4096);
    }

    private static MachineRequest request(String name, int cpus, int memoryMb) {
        return new MachineRequest(name, MachineDistribution.UBUNTU, "24.04", cpus, memoryMb, 20, null, "admin", "segredo", true);
    }

    private static Machine existing(String name, int memoryMb, String address) {
        Machine machine = new Machine(request(name, 1, memoryMb), "D:\\", address, MachineService.macFor(address));
        machine.changeStatus(MachineStatus.RUNNING);
        return machine;
    }

    @Test
    void macAddressIsTheHyperVRangeWithTheMachineAddress() {
        assertEquals("00:15:5D:4D:00:0A", MachineService.macFor("10.77.0.10"));
        assertEquals("00:15:5D:4D:00:FE", MachineService.macFor("10.77.0.254"));
    }

    @Test
    void creationStopsBeforeAnythingWhenHyperVIsNotReady() {
        when(hyperVService.requireReady()).thenThrow(new IllegalStateException("O Hyper-V não está ativo neste PC."));

        assertThrows(IllegalStateException.class, () -> machineService.create(request("web", 2, 2048)));
        verify(machineRepository, never()).save(any());
        verify(credentialService, never()).create(any());
    }

    @Test
    void memoryLeftForWindowsAndOtherMachinesIsNeverPromised() {
        when(machineRepository.findByStatusNot(MachineStatus.REMOVED))
            .thenReturn(List.of(existing("a", 4096, "10.77.0.10"), existing("b", 4096, "10.77.0.11")));

        // 16 GB - 4 GB for Windows - 8 GB of machines = 4 GB left
        machineService.requireCapacity(READY, 2, 4096);
        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> machineService.requireCapacity(READY, 2, 4097));
        assertTrue(refused.getMessage().contains("sobram 4096 MB"), refused.getMessage());
        assertThrows(IllegalArgumentException.class, () -> machineService.requireCapacity(READY, 13, 512));
    }

    @Test
    void onlyOfferedVersionsAreAccepted() {
        machineService.validateSystem(MachineDistribution.DEBIAN, "12");
        assertThrows(IllegalArgumentException.class, () -> machineService.validateSystem(MachineDistribution.DEBIAN, "10"));
    }

    @Test
    void distributionsListNewestVersionFirst() {
        assertEquals(List.of("24.04", "22.04"), MachineDistribution.UBUNTU.getVersions());
        assertEquals(List.of("13", "12"), MachineDistribution.DEBIAN.getVersions());
    }
}
