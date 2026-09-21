package com.bancada.filter;

import com.bancada.enums.OperationStatus;
import com.bancada.enums.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Filtros do histórico de operações")
public class OperationFilter {

    @Schema(description = "Dispositivo", example = "1")
    private Long deviceId;

    @Schema(description = "Tipos de operação")
    private List<OperationType> type;

    @Schema(description = "Situações")
    private List<OperationStatus> status;

    @Schema(description = "Criadas a partir de (inclusive)", example = "2026-09-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Schema(description = "Criadas até (inclusive)", example = "2026-09-30")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public List<OperationType> getType() {
        return type;
    }

    public void setType(List<OperationType> type) {
        this.type = type;
    }

    public List<OperationStatus> getStatus() {
        return status;
    }

    public void setStatus(List<OperationStatus> status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
