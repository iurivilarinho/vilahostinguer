package com.bancada.service;

import com.bancada.enums.DeviceEventType;
import com.bancada.enums.OperationStatus;
import com.bancada.enums.OperationType;
import com.bancada.filter.OperationFilter;
import com.bancada.models.Device;
import com.bancada.models.Operation;
import com.bancada.repository.OperationRepository;
import com.bancada.specification.OperationSpecification;
import com.jcraft.jsch.Channel;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.ToIntFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs long device work in the background and records it. The output is buffered in memory and
 * flushed to the database about once a second, so a chatty {@code apt-get} does not turn into
 * thousands of writes.
 */
@Service
public class OperationService {

    private static final Logger LOG = LoggerFactory.getLogger(OperationService.class);
    private static final int MAX_LOG_CHARS = 400_000;

    private final OperationRepository operationRepository;
    private final EventService eventService;
    private final Executor operationExecutor;
    private final Map<Long, StringBuilder> pendingOutput = new ConcurrentHashMap<>();
    private final Map<Long, Channel> runningChannels = new ConcurrentHashMap<>();
    private final Set<Long> cancelRequests = ConcurrentHashMap.newKeySet();

    public OperationService(OperationRepository operationRepository, EventService eventService,
                            @Qualifier("operationExecutor") Executor operationExecutor) {
        this.operationRepository = operationRepository;
        this.eventService = eventService;
        this.operationExecutor = operationExecutor;
    }

    /** Operations left running by a previous run of the app can never finish: close them as failed. */
    @PostConstruct
    public void closeInterrupted() {
        List<Operation> interrupted = operationRepository.findByStatusIn(List.of(OperationStatus.PENDING, OperationStatus.RUNNING));
        for (Operation operation : interrupted) {
            operation.appendLog("\n[O painel foi fechado durante a execução.]\n");
            operation.finish(OperationStatus.FAILED, null);
            operationRepository.save(operation);
        }
    }

    @Transactional(readOnly = true)
    public Page<Operation> search(OperationFilter filter, Pageable pageable) {
        Specification<Operation> specification = Specification.where(OperationSpecification.device(filter.getDeviceId()))
            .and(OperationSpecification.typeIn(filter.getType()))
            .and(OperationSpecification.statusIn(filter.getStatus()))
            .and(OperationSpecification.createdBetween(filter.getStartDate(), filter.getEndDate()));
        return operationRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Operation findById(Long id) {
        return operationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Operação não encontrada para ID: " + id));
    }

    /**
     * Queues {@code work} for the device. The work receives the operation id and returns the exit
     * code; it reports output through {@link #appendOutput} and registers its SSH channel through
     * {@link #registerChannel} so that it can be canceled.
     */
    public Operation start(Device device, OperationType type, String title, String target, ToIntFunction<Long> work) {
        Operation operation = operationRepository.save(new Operation(device, type, title, target));
        Long operationId = operation.getId();
        operationExecutor.execute(() -> execute(operationId, work));
        return operation;
    }

    public void appendOutput(Long operationId, String chunk) {
        pendingOutput.computeIfAbsent(operationId, id -> new StringBuilder()).append(chunk);
    }

    public void registerChannel(Long operationId, Channel channel) {
        runningChannels.put(operationId, channel);
        if (cancelRequests.contains(operationId)) {
            channel.disconnect();
        }
    }

    public boolean isCancelRequested(Long operationId) {
        return cancelRequests.contains(operationId);
    }

    public Operation cancel(Long id) {
        Operation operation = findById(id);
        if (operation.getStatus().isFinished()) {
            throw new IllegalStateException("A operação já terminou.");
        }
        cancelRequests.add(id);
        Channel channel = runningChannels.get(id);
        if (channel != null) {
            channel.disconnect();
        }
        return operation;
    }

    @Scheduled(fixedDelay = 1_000)
    public void flushPendingOutput() {
        for (Long operationId : pendingOutput.keySet()) {
            flush(operationId);
        }
    }

    private void execute(Long operationId, ToIntFunction<Long> work) {
        if (cancelRequests.contains(operationId)) {
            finish(operationId, OperationStatus.CANCELED, null);
            return;
        }
        Integer exitCode = null;
        OperationStatus result;
        try {
            // inside the try: a failure to mark it running must end the operation, not leave it pending forever
            Operation operation = findById(operationId);
            operation.start();
            operationRepository.save(operation);
            exitCode = work.applyAsInt(operationId);
            if (cancelRequests.contains(operationId)) {
                result = OperationStatus.CANCELED;
            } else {
                result = exitCode == 0 ? OperationStatus.SUCCEEDED : OperationStatus.FAILED;
            }
        } catch (RuntimeException exception) {
            LOG.warn("Operation {} failed: {}", operationId, exception.getMessage());
            appendOutput(operationId, "\n[Erro] " + exception.getMessage() + "\n");
            result = cancelRequests.contains(operationId) ? OperationStatus.CANCELED : OperationStatus.FAILED;
        }
        finish(operationId, result, exitCode);
    }

    private synchronized void finish(Long operationId, OperationStatus status, Integer exitCode) {
        flush(operationId);
        Operation operation = findById(operationId);
        if (status == OperationStatus.CANCELED) {
            operation.appendLog("\n[Cancelada.]\n");
        }
        operation.finish(status, exitCode);
        Operation saved = operationRepository.save(operation);
        runningChannels.remove(operationId);
        cancelRequests.remove(operationId);
        eventService.publish(DeviceEventType.OPERATION_FINISHED, saved.getDevice().getId(), saved.getDevice().getName(),
            saved.getId(), saved.getTitle() + ": " + saved.getStatus().getDescription().toLowerCase());
    }

    private synchronized void flush(Long operationId) {
        StringBuilder buffer = pendingOutput.remove(operationId);
        if (buffer == null || buffer.isEmpty()) {
            return;
        }
        Operation operation = findById(operationId);
        String current = operation.getLog() == null ? "" : operation.getLog();
        if (current.length() + buffer.length() > MAX_LOG_CHARS) {
            if (current.length() < MAX_LOG_CHARS) {
                operation.appendLog(buffer.substring(0, Math.max(0, MAX_LOG_CHARS - current.length()))
                    + "\n[Saída truncada: muito longa para guardar.]\n");
            }
        } else {
            operation.appendLog(buffer.toString());
        }
        operationRepository.save(operation);
    }
}
