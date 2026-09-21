package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bancada.enums.FileSystemType;
import com.bancada.enums.OperationType;
import com.bancada.models.Device;
import com.bancada.records.CommandResult;
import com.bancada.request.FormatPartitionRequest;
import com.bancada.response.PartitionResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StorageServiceTest {

    /** Output of partitions.sh on the J4+: eMMC, system partitions, root, userdata and an SD card. */
    private static final String PHONE_PARTITIONS = String.join("\n",
        "mmcblk0|31268536320|1|||||",
        "mmcblk0p23|33554432|0|||boot||",
        "mmcblk0p31|33554432|0|ext4||persist|/persist|1200000",
        "mmcblk0p44|104857600|0|vfat||modem||",
        "mmcblk0p47|2621440000|0|ext4|pmOS_root|system|/|139264000",
        "mmcblk0p53|27470789632|0|ext4|dados|userdata||",
        "mmcblk1p1|15931539456|0|vfat|SDCARD|||",
        "");

    @Mock
    private DeviceService deviceService;

    @Mock
    private SshService sshService;

    @Mock
    private DeviceScriptService deviceScriptService;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private StorageService storageService;

    private final Device device = new Device("169.254.1.1", 22, "SHA256:abc", "ssh-ed25519", null, null);

    @BeforeEach
    void setUp() {
        when(deviceService.requireReady(1L)).thenReturn(device);
        when(deviceScriptService.load("partitions")).thenReturn("script");
    }

    private Map<String, PartitionResponse> listWith(String output) {
        when(sshService.run(eq(device), anyString(), anyList(), anyBoolean(), any(Duration.class)))
            .thenReturn(new CommandResult(0, output, ""));
        return storageService.partitions(1L).stream().collect(Collectors.toMap(PartitionResponse::name, Function.identity()));
    }

    @Test
    void onlyUnmountedDataPartitionsAreFormattable() {
        Map<String, PartitionResponse> partitions = listWith(PHONE_PARTITIONS);

        assertTrue(partitions.get("mmcblk0p53").formattable());
        assertTrue(partitions.get("mmcblk1p1").formattable());
        assertFalse(partitions.get("mmcblk0").formattable());
        assertFalse(partitions.get("mmcblk0p23").formattable());
        assertFalse(partitions.get("mmcblk0p44").formattable());
        assertFalse(partitions.get("mmcblk0p47").formattable());
        assertEquals("Está montada em /.", partitions.get("mmcblk0p47").protectionReason());
    }

    @Test
    void everythingIsProtectedWhenNoMountPointCouldBeRead() {
        String withoutMounts = "sda|120034123776|1|||||\nsda1|104857600|0|||||\n";

        Map<String, PartitionResponse> partitions = listWith(withoutMounts);

        assertFalse(partitions.get("sda1").formattable());
    }

    @Test
    void formatRefusesAWrongConfirmationBeforeTouchingTheDevice() {
        FormatPartitionRequest request = new FormatPartitionRequest("mmcblk0p53", FileSystemType.EXT4, "dados", "mmcblk0p47");

        assertThrows(IllegalArgumentException.class, () -> storageService.format(1L, request));
        verify(deviceService, never()).requireReady(1L);
    }

    @Test
    void formatRefusesAProtectedPartition() {
        listWith(PHONE_PARTITIONS);
        FormatPartitionRequest request = new FormatPartitionRequest("mmcblk0p44", FileSystemType.VFAT, "", "mmcblk0p44");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> storageService.format(1L, request));
        assertTrue(exception.getMessage().contains("modem"));
        verify(operationService, never()).start(any(), any(),
            anyString(), anyString(), any());
    }

    @Test
    void formatStartsAnOperationForADataPartition() {
        listWith(PHONE_PARTITIONS);
        FormatPartitionRequest request = new FormatPartitionRequest("mmcblk0p53", FileSystemType.EXT4, "dados", "mmcblk0p53");

        storageService.format(1L, request);

        verify(operationService).start(eq(device), eq(OperationType.FORMAT_PARTITION),
            eq("Formatar mmcblk0p53 como ext4"), eq("mmcblk0p53"), any());
    }
}
