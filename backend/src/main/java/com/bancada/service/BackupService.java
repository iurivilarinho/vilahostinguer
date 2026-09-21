package com.bancada.service;

import com.bancada.enums.BackupStatus;
import com.bancada.enums.OperationType;
import com.bancada.filter.BackupFilter;
import com.bancada.models.Backup;
import com.bancada.models.Device;
import com.bancada.models.Operation;
import com.bancada.records.CommandResult;
import com.bancada.repository.BackupRepository;
import com.bancada.request.BackupRequest;
import com.bancada.specification.BackupSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backups are tar.gz streams produced on the device and written straight to a file here, hashing
 * on the way. Nothing is staged on the device, which matters on phones with little free space.
 */
@Service
public class BackupService {

    private static final Logger LOG = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Set<String> FORBIDDEN_ROOTS = Set.of("/", "/proc", "/sys", "/dev", "/run", "/tmp");
    /** GNU tar exits with 1 when a file changed while being read; the archive is still usable. */
    private static final int TAR_WARNING_EXIT = 1;

    private final BackupRepository backupRepository;
    private final DeviceService deviceService;
    private final SettingsService settingsService;
    private final SshService sshService;
    private final OperationService operationService;

    public BackupService(BackupRepository backupRepository, DeviceService deviceService, SettingsService settingsService,
                         SshService sshService, OperationService operationService) {
        this.backupRepository = backupRepository;
        this.deviceService = deviceService;
        this.settingsService = settingsService;
        this.sshService = sshService;
        this.operationService = operationService;
    }

    @Transactional(readOnly = true)
    public Page<Backup> search(BackupFilter filter, Pageable pageable) {
        Specification<Backup> specification = Specification.where(BackupSpecification.device(filter.getDeviceId()))
            .and(BackupSpecification.search(filter.getSearch()))
            .and(BackupSpecification.statusIn(filter.getStatus()));
        return backupRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Backup findById(Long id) {
        return backupRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Backup não encontrado para ID: " + id));
    }

    public Backup create(BackupRequest request) {
        Device device = deviceService.requireReady(request.deviceId());
        List<String> paths = request.paths().stream().map(BackupService::normalizePath).distinct().toList();
        Path file = Paths.get(settingsService.get().getBackupDirectory(), String.valueOf(device.getId()),
            FILE_STAMP.format(LocalDateTime.now()) + "-" + slug(request.name()) + ".tar.gz");
        Backup backup = backupRepository.save(new Backup(device, request.name().trim(), paths, file.toString()));
        Long backupId = backup.getId();
        Operation operation = operationService.start(device, OperationType.BACKUP, "Backup: " + backup.getName(),
            "backup:" + backupId, operationId -> produce(backupId, device, paths, file, operationId));
        // Only for the response: the link is persisted by the job itself, so this detached copy is never saved.
        backup.setOperation(operation);
        return backup;
    }

    public Operation restore(Long id) {
        Backup backup = findById(id);
        if (backup.getStatus() != BackupStatus.AVAILABLE) {
            throw new IllegalStateException("Só backups disponíveis podem ser restaurados.");
        }
        Path file = Paths.get(backup.getFilePath());
        if (!Files.isReadable(file)) {
            throw new IllegalStateException("O arquivo do backup não está mais em " + file + ".");
        }
        Device device = deviceService.requireReady(backup.getDevice().getId());
        return operationService.start(device, OperationType.RESTORE, "Restaurar: " + backup.getName(), "backup:" + id,
            operationId -> {
                operationService.appendOutput(operationId, "== Restaurando " + String.join(", ", backup.getPaths())
                    + " em " + device.getName() + " ==\n");
                try (InputStream input = Files.newInputStream(file)) {
                    CommandResult result = sshService.upload(device, "tar xzf - -C /", true, input,
                        channel -> operationService.registerChannel(operationId, channel));
                    appendIfPresent(operationId, result.output());
                    appendIfPresent(operationId, result.errorOutput());
                    operationService.appendOutput(operationId, result.succeeded() ? "== Concluído ==\n" : "");
                    return result.exitCode();
                } catch (IOException exception) {
                    throw new IllegalStateException("Não foi possível ler o arquivo do backup: " + exception.getMessage(), exception);
                }
            });
    }

    @Transactional
    public Backup changeStatus(Long id, BackupStatus target) {
        Backup backup = findById(id);
        backup.changeStatus(target);
        if (target == BackupStatus.DISCARDED && backup.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(backup.getFilePath()));
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível apagar o arquivo do backup: " + exception.getMessage(), exception);
            }
        }
        return backupRepository.save(backup);
    }

    public Path file(Long id) {
        Backup backup = findById(id);
        Path file = backup.getFilePath() == null ? null : Paths.get(backup.getFilePath());
        if (backup.getStatus() != BackupStatus.AVAILABLE || file == null || !Files.isReadable(file)) {
            throw new IllegalStateException("O arquivo deste backup não está disponível.");
        }
        return file;
    }

    private int produce(Long backupId, Device device, List<String> paths, Path file, Long operationId) {
        Backup linked = findById(backupId);
        linked.setOperation(operationService.findById(operationId));
        backupRepository.save(linked);
        operationService.appendOutput(operationId, "== Gerando backup de " + String.join(", ", paths) + " ==\n");
        StringBuilder list = new StringBuilder();
        paths.forEach(path -> list.append(' ').append(SshService.quote(path.substring(1))));
        String script = "cd / || exit 1\nset --\nfor item in" + list + "; do\n"
            + "\tif [ -e \"$item\" ]; then set -- \"$@\" \"$item\"; else echo \"Ignorando /$item (não existe)\" >&2; fi\n"
            + "done\n[ $# -gt 0 ] || { echo 'Nenhuma das pastas existe no dispositivo.' >&2; exit 2; }\n"
            + "tar czf - \"$@\"\n";
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        CommandResult result;
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = new DigestOutputStream(new BufferedOutputStream(Files.newOutputStream(file)), digest)) {
                result = sshService.download(device, script, true, output,
                    channel -> operationService.registerChannel(operationId, channel));
            }
            appendIfPresent(operationId, result.errorOutput());
            long size = Files.size(file);
            boolean usable = result.exitCode() == 0 || (result.exitCode() == TAR_WARNING_EXIT && size > 0);
            if (usable && !operationService.isCancelRequested(operationId)) {
                Backup backup = findById(backupId);
                backup.complete(size, HexFormat.of().formatHex(digest.digest()));
                backupRepository.save(backup);
                operationService.appendOutput(operationId, "== Salvo em " + file + " (" + size + " bytes) ==\n");
                return 0;
            }
        } catch (IOException exception) {
            operationService.appendOutput(operationId, "Erro ao gravar o arquivo: " + exception.getMessage() + "\n");
            result = new CommandResult(1, "", "");
        } catch (RuntimeException exception) {
            markFailed(backupId, file);
            throw exception;
        }
        markFailed(backupId, file);
        return result.exitCode() == 0 ? 1 : result.exitCode();
    }

    private void markFailed(Long backupId, Path file) {
        Backup backup = findById(backupId);
        backup.changeStatus(BackupStatus.FAILED);
        backupRepository.save(backup);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            LOG.warn("Could not delete partial backup {}: {}", file, exception.getMessage());
        }
    }

    private void appendIfPresent(Long operationId, String text) {
        if (text != null && !text.isBlank()) {
            operationService.appendOutput(operationId, text.endsWith("\n") ? text : text + "\n");
        }
    }

    private static String normalizePath(String path) {
        String trimmed = path.trim().replace('\\', '/');
        if (!trimmed.startsWith("/") || trimmed.contains("..")) {
            throw new IllegalArgumentException("Caminho inválido: " + path + ". Use caminhos absolutos, como /etc.");
        }
        String normalized = trimmed.length() > 1 && trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        if (FORBIDDEN_ROOTS.contains(normalized)) {
            throw new IllegalArgumentException("A pasta " + normalized + " não pode entrar num backup.");
        }
        return normalized;
    }

    private static String slug(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "backup" : slug.substring(0, Math.min(slug.length(), 40));
    }
}
