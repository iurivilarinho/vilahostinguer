package com.bancada.service;

import com.bancada.hyperv.PowerShell;
import com.bancada.records.CommandResult;
import com.bancada.records.HyperVStatus;
import com.bancada.records.VirtualMachineState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Hyper-V on this PC, through PowerShell. The panel only touches the virtual machines it created
 * (named {@code bancada-*}); everything runs as the Windows user of the panel, who must be in the
 * "Hyper-V Administrators" group (tools\habilitar-hyperv.ps1 does it).
 */
@Service
public class HyperVService {

    public static final String VM_PREFIX = "bancada-";
    private static final Duration QUICK = Duration.ofSeconds(60);
    private static final Duration SLOW = Duration.ofMinutes(15);

    private static final long STATUS_CACHE_MS = 60_000;

    private final ObjectMapper objectMapper;
    private volatile CachedStatus cached;
    private final String switchName;
    private final String network;

    public HyperVService(ObjectMapper objectMapper, @Value("${bancada.vms.switch}") String switchName,
                         @Value("${bancada.vms.network}") String network) {
        this.objectMapper = objectMapper;
        this.switchName = switchName;
        this.network = network;
    }

    /** First three octets of the VM network, like {@code 10.77.0}. */
    public String network() {
        return network;
    }

    public String gateway() {
        return network + ".1";
    }

    /** Is Hyper-V there, may this user drive it, and does the Bancada network exist? */
    public HyperVStatus status() {
        String script = "$r = [ordered]@{ installed = $false; permitted = $false; network = $false; memoryMb = 0; cpus = 0; message = $null }\n"
            + "$r.memoryMb = [int]((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1MB)\n"
            + "$r.cpus = [int](Get-CimInstance Win32_ComputerSystem).NumberOfLogicalProcessors\n"
            + "if (-not (Get-Command Get-VM -ErrorAction SilentlyContinue)) {\n"
            + "  $r.message = 'O Hyper-V não está ativo neste PC.'\n"
            + "} else {\n"
            + "  $r.installed = $true\n"
            + "  try {\n"
            + "    Get-VMHost | Out-Null\n"
            + "    $r.permitted = $true\n"
            + "    $sw = Get-VMSwitch -Name " + PowerShell.literal(switchName) + " -ErrorAction SilentlyContinue\n"
            + "    $ip = Get-NetIPAddress -IPAddress " + PowerShell.literal(gateway()) + " -ErrorAction SilentlyContinue\n"
            + "    $r.network = [bool]$sw -and [bool]$ip\n"
            + "    if (-not $r.network) { $r.message = 'A rede das máquinas (switch " + switchName + " em " + network + ".0/24) ainda não existe.' }\n"
            + "  } catch {\n"
            + "    $r.message = 'Este usuário do Windows não tem permissão no Hyper-V.'\n"
            + "  }\n"
            + "}\n"
            + "$r | ConvertTo-Json -Compress\n";
        CommandResult result = PowerShell.run(script, QUICK);
        JsonNode json = parse(result);
        return new HyperVStatus(json.path("installed").asBoolean(), json.path("permitted").asBoolean(), json.path("network").asBoolean(),
            json.path("memoryMb").asLong(), json.path("cpus").asInt(), text(json, "message"), switchName, network + ".0/24");
    }

    /** {@link #status()} at most a minute old: the storefront asks for it on every page. */
    public HyperVStatus cachedStatus() {
        CachedStatus cached = this.cached;
        if (cached == null || cached.readAt() < System.currentTimeMillis() - STATUS_CACHE_MS) {
            cached = new CachedStatus(status(), System.currentTimeMillis());
            this.cached = cached;
        }
        return cached.status();
    }

    private record CachedStatus(HyperVStatus status, long readAt) {
    }

    /** Refuses with the reason when machines cannot be created or controlled now. */
    public HyperVStatus requireReady() {
        HyperVStatus status = status();
        if (!status.ready()) {
            throw new IllegalStateException(status.message() + " Rode tools\\habilitar-hyperv.ps1 como administrador (veja o README).");
        }
        return status;
    }

    /** State and use of every machine of the panel. */
    public List<VirtualMachineState> list() {
        String script = "$vms = @(Get-VM -Name '" + VM_PREFIX + "*' -ErrorAction SilentlyContinue | ForEach-Object {\n"
            + "  [ordered]@{ name = $_.Name; state = $_.State.ToString(); cpu = [int]$_.CPUUsage;\n"
            + "    memoryMb = [long]($_.MemoryAssigned / 1MB); uptime = [long]$_.Uptime.TotalSeconds } })\n"
            + "ConvertTo-Json -InputObject $vms -Compress -Depth 3\n";
        JsonNode json = parse(PowerShell.run(script, QUICK));
        List<VirtualMachineState> states = new ArrayList<>();
        for (JsonNode vm : json) {
            states.add(new VirtualMachineState(vm.path("name").asText(), vm.path("state").asText(), vm.path("cpu").asInt(),
                vm.path("memoryMb").asLong(), vm.path("uptime").asLong()));
        }
        return states;
    }

    /**
     * Copies the base disk of the distribution to the machine folder and grows it to the chosen size;
     * the system grows its partition on the first boot (cloud-init growpart).
     */
    public int prepareDisk(Path baseDisk, Path disk, long sizeBytes, Consumer<String> log) {
        String script = "New-Item -ItemType Directory -Force -Path " + PowerShell.literal(disk.getParent().toString()) + " | Out-Null\n"
            + "Write-Output '== Copiando o disco base =='\n"
            + "Copy-Item -Force -Path " + PowerShell.literal(baseDisk.toString()) + " -Destination " + PowerShell.literal(disk.toString()) + "\n"
            + "$current = (Get-VHD -Path " + PowerShell.literal(disk.toString()) + ").Size\n"
            + "if (" + sizeBytes + " -gt $current) {\n"
            + "  Write-Output ('== Aumentando o disco para ' + [math]::Round(" + sizeBytes + " / 1GB) + ' GB ==')\n"
            + "  Resize-VHD -Path " + PowerShell.literal(disk.toString()) + " -SizeBytes " + sizeBytes + "\n"
            + "}\n";
        return PowerShell.stream(script, log, process -> { });
    }

    /** The cloud-init NoCloud ISO (label cidata) with the given files. */
    public void buildSeedIso(Map<String, String> files, Path iso) {
        Path folder = null;
        try {
            folder = Files.createTempDirectory("bancada-seed");
            for (Map.Entry<String, String> file : files.entrySet()) {
                Files.writeString(folder.resolve(file.getKey()), file.getValue().replace("\r\n", "\n"), StandardCharsets.UTF_8);
            }
            Files.createDirectories(iso.getParent());
            Files.deleteIfExists(iso);
            String script = "$SeedFolder = " + PowerShell.literal(folder.toString()) + "\n$IsoPath = " + PowerShell.literal(iso.toString()) + "\n"
                + resource("hyperv/seed-iso.ps1");
            CommandResult result = PowerShell.run(script, QUICK);
            if (!result.succeeded()) {
                throw new IllegalStateException("Não foi possível gerar o disco de configuração: " + firstLine(result.errorOutput()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o disco de configuração: " + exception.getMessage(), exception);
        } finally {
            deleteTree(folder);
        }
    }

    /** New generation 2 VM with fixed memory, secure boot for Linux, the seed ISO and a fixed MAC; then starts it. */
    public int create(String vmName, Path folder, Path disk, Path iso, int cpus, int memoryMb, String macAddress, boolean autoStart,
                      Consumer<String> log) {
        String name = PowerShell.literal(vmName);
        String script = "if (Get-VM -Name " + name + " -ErrorAction SilentlyContinue) { throw 'Já existe uma VM chamada " + vmName + " no Hyper-V.' }\n"
            + "Write-Output '== Criando a VM no Hyper-V =='\n"
            + "$vm = New-VM -Name " + name + " -Generation 2 -MemoryStartupBytes " + memoryMb + "MB -VHDPath " + PowerShell.literal(disk.toString())
            + " -SwitchName " + PowerShell.literal(switchName) + " -Path " + PowerShell.literal(folder.getParent().toString()) + "\n"
            + "Set-VMMemory -VM $vm -DynamicMemoryEnabled $false\n"
            + "Set-VMProcessor -VM $vm -Count " + cpus + "\n"
            + "Set-VMFirmware -VM $vm -EnableSecureBoot On -SecureBootTemplate MicrosoftUEFICertificateAuthority\n"
            + "Add-VMDvdDrive -VM $vm -Path " + PowerShell.literal(iso.toString()) + "\n"
            + "Set-VMNetworkAdapter -VM $vm -StaticMacAddress " + PowerShell.literal(macAddress.replace(":", "")) + "\n"
            + "Set-VMFirmware -VM $vm -FirstBootDevice (Get-VMHardDiskDrive -VM $vm)\n"
            + "Set-VM -VM $vm -AutomaticStartAction " + (autoStart ? "Start" : "Nothing") + " -AutomaticStopAction ShutDown"
            + " -AutomaticCheckpointsEnabled $false -CheckpointType Production\n"
            + "Write-Output '== Ligando a VM =='\n"
            + "Start-VM -VM $vm\n";
        return PowerShell.stream(script, log, process -> { });
    }

    public void start(String vmName) {
        run("Start-VM -Name " + PowerShell.literal(vmName), "ligar");
    }

    /** Clean shutdown through the guest tools; {@code force} pulls the plug. */
    public void stop(String vmName, boolean force) {
        run("Stop-VM -Name " + PowerShell.literal(vmName) + (force ? " -TurnOff" : " -Force"), "desligar");
    }

    public void restart(String vmName) {
        run("$vm = Get-VM -Name " + PowerShell.literal(vmName) + "\nif ($vm.State -eq 'Running') { Stop-VM -VM $vm -Force }\nStart-VM -VM $vm", "reiniciar");
    }

    /** Turns the VM off, removes it from Hyper-V and erases its folder (disk, seed, configuration). */
    public void remove(String vmName, Path folder) {
        run("$vm = Get-VM -Name " + PowerShell.literal(vmName) + " -ErrorAction SilentlyContinue\n"
            + "if ($vm) {\n  if ($vm.State -ne 'Off') { Stop-VM -VM $vm -TurnOff -Force }\n  Remove-VM -VM $vm -Force\n}\n"
            + "if (Test-Path " + PowerShell.literal(folder.toString()) + ") { Remove-Item -Recurse -Force " + PowerShell.literal(folder.toString()) + " }",
            "remover");
    }

    /** Reinstall: off, fresh disk from the base image, new seed, on. */
    public int reinstall(String vmName, Path baseDisk, Path disk, long sizeBytes, Path iso, Consumer<String> log) {
        String name = PowerShell.literal(vmName);
        String script = "$vm = Get-VM -Name " + name + "\n"
            + "Write-Output '== Desligando a VM =='\n"
            + "if ($vm.State -ne 'Off') { Stop-VM -VM $vm -TurnOff -Force }\n"
            + "Get-VMSnapshot -VM $vm | Remove-VMSnapshot\n"
            + "Write-Output '== Trocando o disco pelo sistema novo =='\n"
            + "Copy-Item -Force -Path " + PowerShell.literal(baseDisk.toString()) + " -Destination " + PowerShell.literal(disk.toString()) + "\n"
            + "if (" + sizeBytes + " -gt (Get-VHD -Path " + PowerShell.literal(disk.toString()) + ").Size) { Resize-VHD -Path "
            + PowerShell.literal(disk.toString()) + " -SizeBytes " + sizeBytes + " }\n"
            + "Get-VMDvdDrive -VM $vm | Set-VMDvdDrive -Path " + PowerShell.literal(iso.toString()) + "\n"
            + "Write-Output '== Ligando a VM =='\n"
            + "Start-VM -VM $vm\n";
        return PowerShell.stream(script, log, process -> { });
    }

    /**
     * Copy of the disk as it is now. A running VM gets a production checkpoint (the guest tools
     * freeze the file systems), the frozen disk is copied, and the checkpoint is merged back.
     */
    public int backupDisk(String vmName, Path disk, Path target, Consumer<String> log) {
        String name = PowerShell.literal(vmName);
        String script = "$vm = Get-VM -Name " + name + "\n"
            + "$checkpoint = $null\n"
            + "if ($vm.State -ne 'Off') {\n"
            + "  Write-Output '== Congelando o disco (checkpoint) =='\n"
            + "  $checkpoint = Checkpoint-VM -VM $vm -SnapshotName 'bancada-backup' -Passthru\n"
            + "}\n"
            + "try {\n"
            + "  Write-Output '== Copiando o disco =='\n"
            + "  New-Item -ItemType Directory -Force -Path " + PowerShell.literal(target.getParent().toString()) + " | Out-Null\n"
            + "  Copy-Item -Force -Path " + PowerShell.literal(disk.toString()) + " -Destination " + PowerShell.literal(target.toString()) + "\n"
            + "} finally {\n"
            + "  if ($checkpoint) { Write-Output '== Juntando o checkpoint de volta =='; Remove-VMSnapshot -VMSnapshot $checkpoint }\n"
            + "}\n";
        return PowerShell.stream(script, log, process -> { });
    }

    /** Puts a backup copy back as the disk of the VM and turns it on. */
    public int restoreDisk(String vmName, Path backup, Path disk, Consumer<String> log) {
        String script = "$vm = Get-VM -Name " + PowerShell.literal(vmName) + "\n"
            + "Write-Output '== Desligando a VM =='\n"
            + "if ($vm.State -ne 'Off') { Stop-VM -VM $vm -TurnOff -Force }\n"
            + "Get-VMSnapshot -VM $vm | Remove-VMSnapshot\n"
            + "Write-Output '== Voltando o disco do backup =='\n"
            + "Copy-Item -Force -Path " + PowerShell.literal(backup.toString()) + " -Destination " + PowerShell.literal(disk.toString()) + "\n"
            + "Write-Output '== Ligando a VM =='\n"
            + "Start-VM -VM $vm\n";
        return PowerShell.stream(script, log, process -> { });
    }

    /** Fixed VHD made from the cloud image, into the dynamic VHDX every machine is copied from. */
    public int convertToVhdx(Path vhd, Path vhdx, Consumer<String> log) {
        String script = "Write-Output '== Convertendo para VHDX =='\n"
            + "Convert-VHD -Path " + PowerShell.literal(vhd.toString()) + " -DestinationPath " + PowerShell.literal(vhdx.toString())
            + " -VHDType Dynamic\n";
        return PowerShell.stream(script, log, process -> { });
    }

    /** Bytes the dynamic disk takes on the PC now. */
    public static long diskFileSize(Path disk) {
        try {
            return Files.size(disk);
        } catch (IOException exception) {
            return 0;
        }
    }

    private void run(String script, String action) {
        CommandResult result = PowerShell.run(script, SLOW);
        if (!result.succeeded()) {
            throw new IllegalStateException("O Hyper-V não conseguiu " + action + " a máquina: " + firstLine(result.errorOutput()));
        }
    }

    private JsonNode parse(CommandResult result) {
        if (!result.succeeded()) {
            throw new IllegalStateException("O PowerShell falhou: " + firstLine(result.errorOutput()));
        }
        try {
            String output = result.output().trim();
            return objectMapper.readTree(output.isEmpty() ? "[]" : output);
        } catch (IOException exception) {
            throw new IllegalStateException("Resposta inesperada do PowerShell: " + firstLine(result.output()), exception);
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode value = json.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    /** First meaningful line of a PowerShell error (the rest is the position of the error). */
    static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("At line") && !trimmed.startsWith("No linha") && !trimmed.startsWith("+")) {
                return trimmed;
            }
        }
        return text.trim();
    }

    private static String resource(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Script interno ausente: " + path, exception);
        }
    }

    private static void deleteTree(Path folder) {
        if (folder == null) {
            return;
        }
        try (var paths = Files.walk(folder)) {
            paths.sorted((first, second) -> second.getNameCount() - first.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // temporary folder; Windows cleans it eventually
                }
            });
        } catch (IOException ignored) {
            // nothing to clean
        }
    }
}
