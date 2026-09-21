package com.bancada.service;

import com.bancada.enums.CatalogApp;
import com.bancada.enums.InitSystem;
import com.bancada.enums.OperationType;
import com.bancada.enums.PackageManager;
import com.bancada.enums.ServiceAction;
import com.bancada.models.Device;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.records.KeyValueOutput;
import com.bancada.response.DeviceAppResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Installs and controls the catalog applications. Every command line ends with {@code || exit $?}
 * instead of relying on {@code set -e}, which ignores a failure in the middle of an {@code &&} chain
 * like {@code apk update && apk add nginx}.
 */
@Service
public class AppCatalogService {

    private static final Duration STATUS_TIMEOUT = Duration.ofSeconds(45);
    private static final String SECTION_MARK = "@@";

    private final DeviceService deviceService;
    private final SshService sshService;
    private final OperationService operationService;
    private final DeviceScriptService deviceScriptService;

    public AppCatalogService(DeviceService deviceService, SshService sshService, OperationService operationService,
                             DeviceScriptService deviceScriptService) {
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.operationService = operationService;
        this.deviceScriptService = deviceScriptService;
    }

    public List<DeviceAppResponse> list(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        PackageManager manager = requirePackageManager(device);
        InitSystem initSystem = device.getInitSystem();

        StringBuilder script = new StringBuilder();
        for (CatalogApp app : CatalogApp.values()) {
            script.append("echo '").append(SECTION_MARK).append(app.name()).append("'\n");
            String packages = app.packagesFor(manager);
            if (packages == null) {
                script.append("echo available=0\n");
                continue;
            }
            // Installed only when every package of the app is: "curl wget htop" with htop missing is not installed.
            String condition = String.join(" && ", Arrays.stream(packages.split(" ")).map(manager::installedCondition).toList());
            script.append("if ").append(condition).append("; then echo installed=1; ")
                .append("command -v ").append(app.getBinary()).append(" >/dev/null 2>&1 && echo \"version=$(")
                .append(app.getVersionCommand()).append(" 2>/dev/null | head -1)\"; ")
                .append("else echo installed=0; fi\n");
            String service = app.serviceFor(manager);
            if (service != null && initSystem != null) {
                script.append(initSystem.statusSnippet(service)).append('\n');
            }
        }
        CommandResult result = sshService.run(device, script.toString(), List.of(), false, STATUS_TIMEOUT);
        Map<String, KeyValueOutput> sections = splitSections(result.output());

        List<DeviceAppResponse> apps = new ArrayList<>();
        for (CatalogApp app : CatalogApp.values()) {
            KeyValueOutput section = sections.getOrDefault(app.name(), KeyValueOutput.parse(""));
            boolean available = app.packagesFor(manager) != null;
            String service = available && initSystem != null ? app.serviceFor(manager) : null;
            apps.add(new DeviceAppResponse(app, app.getDisplayName(), app.getDescription(), app.getCategory(),
                app.getCategory().getDescription(), available, section.flag("installed"), section.text("version"),
                service, section.flag("running"), section.flag("enabled")));
        }
        return apps;
    }

    public Operation install(Long deviceId, CatalogApp app) {
        Device device = deviceService.requireReady(deviceId);
        PackageManager manager = requirePackageManager(device);
        String packages = requirePackages(app, manager);
        StringBuilder script = new StringBuilder()
            .append("echo '== Instalando ").append(app.getDisplayName()).append(" (").append(packages).append(") =='\n")
            .append(manager.installCommand(packages)).append(" || exit $?\n");
        String postInstall = app.postInstallFor(manager);
        if (postInstall != null) {
            script.append("echo '== Preparando ").append(app.getDisplayName()).append(" =='\n").append(postInstall).append('\n');
        }
        if (app == CatalogApp.DOCKER) {
            script.append(deviceScriptService.load("docker-setup")).append('\n');
        }
        String service = app.serviceFor(manager);
        InitSystem initSystem = device.getInitSystem();
        if (service != null && initSystem != null) {
            script.append("echo '== Ativando o serviço ").append(service).append(" =='\n")
                .append(initSystem.serviceCommand(service, ServiceAction.ENABLE)).append(" || true\n")
                .append(initSystem.serviceCommand(service, ServiceAction.START))
                .append(" || echo 'Aviso: o serviço não iniciou. Veja a configuração e tente iniciar pelo painel.'\n");
        }
        script.append("echo '== Concluído =='\n");
        return run(device, OperationType.INSTALL_APP, "Instalar " + app.getDisplayName(), app.name(), script.toString());
    }

    public Operation remove(Long deviceId, CatalogApp app) {
        Device device = deviceService.requireReady(deviceId);
        PackageManager manager = requirePackageManager(device);
        String packages = requirePackages(app, manager);
        StringBuilder script = new StringBuilder();
        String service = app.serviceFor(manager);
        InitSystem initSystem = device.getInitSystem();
        if (service != null && initSystem != null) {
            script.append(initSystem.serviceCommand(service, ServiceAction.STOP)).append(" || true\n")
                .append(initSystem.serviceCommand(service, ServiceAction.DISABLE)).append(" || true\n");
        }
        script.append("echo '== Removendo ").append(app.getDisplayName()).append(" =='\n")
            .append(manager.removeCommand(packages)).append(" || exit $?\n")
            .append("echo '== Concluído =='\n");
        return run(device, OperationType.REMOVE_APP, "Remover " + app.getDisplayName(), app.name(), script.toString());
    }

    public Operation serviceAction(Long deviceId, CatalogApp app, ServiceAction action) {
        Device device = deviceService.requireReady(deviceId);
        PackageManager manager = requirePackageManager(device);
        String service = app.serviceFor(manager);
        if (service == null) {
            throw new IllegalArgumentException(app.getDisplayName() + " não tem serviço para controlar.");
        }
        if (device.getInitSystem() == null) {
            throw new IllegalStateException("Não identifiquei o sistema de inicialização deste dispositivo.");
        }
        String script = device.getInitSystem().serviceCommand(service, action) + " || exit $?\n"
            + "echo '== " + action.getDescription() + ": concluído =='\n";
        return run(device, OperationType.SERVICE_ACTION, action.getDescription() + " " + app.getDisplayName(), app.name(), script);
    }

    public Operation upgradeSystem(Long deviceId) {
        Device device = deviceService.requireReady(deviceId);
        PackageManager manager = requirePackageManager(device);
        String script = "echo '== Atualizando os pacotes do sistema =='\n" + manager.getUpgradeCommand() + " || exit $?\n"
            + "echo '== Concluído =='\n";
        return run(device, OperationType.SYSTEM_UPGRADE, "Atualizar o sistema", manager.getBinary(), script);
    }

    private Operation run(Device device, OperationType type, String title, String target, String script) {
        return operationService.start(device, type, title, target, operationId -> sshService.stream(device, script, true,
            chunk -> operationService.appendOutput(operationId, chunk),
            channel -> operationService.registerChannel(operationId, channel)));
    }

    private static PackageManager requirePackageManager(Device device) {
        if (device.getPackageManager() == null) {
            throw new IllegalStateException("Não identifiquei o gerenciador de pacotes de " + device.getName()
                + ". Atualize as informações do dispositivo.");
        }
        return device.getPackageManager();
    }

    private static String requirePackages(CatalogApp app, PackageManager manager) {
        String packages = app.packagesFor(manager);
        if (packages == null) {
            throw new IllegalArgumentException(app.getDisplayName() + " não está disponível para este sistema.");
        }
        return packages;
    }

    private static Map<String, KeyValueOutput> splitSections(String output) {
        Map<String, KeyValueOutput> sections = new HashMap<>();
        for (String block : output.split(SECTION_MARK)) {
            int lineEnd = block.indexOf('\n');
            if (lineEnd <= 0) {
                continue;
            }
            sections.put(block.substring(0, lineEnd).trim(), KeyValueOutput.parse(block.substring(lineEnd + 1)));
        }
        return sections;
    }
}
