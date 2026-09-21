package com.bancada.records;

import com.bancada.enums.MachineStatus;

/** Published whenever a machine changes status, so other modules (customer subscriptions) can react. */
public record MachineStatusChangedEvent(Long machineId, MachineStatus status) {
}
