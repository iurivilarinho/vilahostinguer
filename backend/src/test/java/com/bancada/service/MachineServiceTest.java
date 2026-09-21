package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.OperationType;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.models.Operation;
import com.bancada.records.DeviceFacts;
import com.bancada.repository.MachineRepository;
import com.bancada.request.MachinePortRequest;
import com.bancada.request.MachineRequest;
import com.bancada.request.MachineVolumeRequest;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private SshService sshService;

    @Mock
    private OperationService operationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private MachineService machineService;

    private final Device phone = new Device("169.254.1.1", 22, "SHA256:abc", "ssh-ed25519", ConnectionType.USB, null);

    @BeforeEach
    void setUp() {
        phone.applyFacts(new DeviceFacts("j4", "postmarketOS", "edge", "3.18.140", "aarch64", null, 4, null, null, null, null,
            "apk", "openrc", "/root", true));
        when(deviceService.requireReady(1L)).thenReturn(phone);
        when(machineRepository.save(any(Machine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operationService.start(any(), any(), anyString(), anyString(), any())).thenReturn(new Operation());
    }

    private MachineRequest request(MachineDistribution distribution, String version, MachineNetworkMode network, Integer sshPort,
                                   String password) {
        return new MachineRequest(1L, "web-teste", distribution, version, 1.5, 512, network,
            List.of(new MachinePortRequest(8080, 80, "tcp")), List.of(new MachineVolumeRequest("/srv/sites", "/var/www")),
            "iuri", password, true, sshPort, true);
    }

    @SuppressWarnings("unchecked")
    private String creationScript(MachineRequest request) {
        machineService.create(request);
        ArgumentCaptor<ToIntFunction<Long>> work = ArgumentCaptor.forClass(ToIntFunction.class);
        verify(operationService).start(eq(phone), eq(OperationType.MACHINE_CREATE), anyString(), anyString(), work.capture());
        when(machineRepository.findById(any())).thenReturn(Optional.of(new Machine(request, phone)));
        ArgumentCaptor<String> script = ArgumentCaptor.forClass(String.class);
        when(sshService.stream(eq(phone), script.capture(), anyBoolean(), any(), any())).thenReturn(0);
        work.getValue().applyAsInt(7L);
        return script.getValue();
    }

    @Test
    void refusesADistributionWithoutImageForTheProcessor() {
        MachineRequest arch = request(MachineDistribution.ARCH, "latest", MachineNetworkMode.HOST, 2201, "segredo");

        assertThrows(IllegalArgumentException.class, () -> machineService.create(arch));
        verify(machineRepository, never()).save(any(Machine.class));
    }

    @Test
    void refusesSshOnPort22OnTheDeviceNetwork() {
        MachineRequest clash = request(MachineDistribution.ALPINE, "3.22", MachineNetworkMode.HOST, 22, "segredo");

        assertThrows(IllegalArgumentException.class, () -> machineService.create(clash));
    }

    @Test
    void hostNetworkScriptHasLimitsSshPortAndNoPortMapping() {
        String script = creationScript(request(MachineDistribution.UBUNTU, "24.04", MachineNetworkMode.HOST, 2201, "segredo"));

        assertTrue(script.contains("docker pull 'ubuntu:24.04'"));
        assertTrue(script.contains("--network host"));
        assertTrue(script.contains("--cpus 1.50"));
        assertTrue(script.contains("--memory 512m"));
        assertTrue(script.contains("--restart unless-stopped"));
        assertTrue(script.contains("-v '/srv/sites:/var/www'"));
        assertTrue(script.contains("Port 2201"));
        assertTrue(script.contains("chown iuri: "), "an empty shared folder goes to the machine user");
        assertTrue(script.contains("docker info 2>&1 | grep -q 'No cpu cfs quota'"));
        assertFalse(script.contains(" -p 8080:80/tcp"), "port mappings make no sense on the host network");
    }

    @Test
    void passwordWithQuotesIsSafelyQuotedForTheShell() {
        String script = creationScript(request(MachineDistribution.DEBIAN, "12", MachineNetworkMode.BRIDGE, 22, "it's $HOME"));

        assertTrue(script.contains("printf '%s:%s\\n' 'iuri' 'it'\\''s $HOME' | docker exec -i 'bancada-web-teste' chpasswd"));
        assertTrue(script.contains(" -p 8080:80/tcp"));
    }

    @Test
    void operationThatDoesNotStartReleasesTheName() {
        MachineRequest request = request(MachineDistribution.ALPINE, "3.22", MachineNetworkMode.HOST, 2201, "segredo");
        Machine stored = new Machine(request, phone);
        when(machineRepository.findById(any())).thenReturn(Optional.of(stored));
        when(operationService.start(any(), any(), anyString(), anyString(), any())).thenThrow(new IllegalStateException("banco recusou"));

        assertThrows(IllegalStateException.class, () -> machineService.create(request));
        assertEquals(MachineStatus.REMOVED, stored.getStatus());
    }

    @Test
    void failedCreationMarksTheMachineAsFailed() {
        MachineRequest request = request(MachineDistribution.ALPINE, "3.22", MachineNetworkMode.HOST, 2201, "segredo");
        Machine stored = new Machine(request, phone);
        machineService.create(request);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ToIntFunction<Long>> work = ArgumentCaptor.forClass(ToIntFunction.class);
        verify(operationService).start(eq(phone), eq(OperationType.MACHINE_CREATE), anyString(), anyString(), work.capture());
        when(machineRepository.findById(any())).thenReturn(Optional.of(stored));
        when(sshService.stream(eq(phone), anyString(), anyBoolean(), any(), any())).thenThrow(new IllegalStateException("cabo saiu"));

        assertThrows(IllegalStateException.class, () -> work.getValue().applyAsInt(7L));
        assertEquals(MachineStatus.FAILED, stored.getStatus());
    }
}
