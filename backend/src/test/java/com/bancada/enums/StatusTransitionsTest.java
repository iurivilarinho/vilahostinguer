package com.bancada.enums;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusTransitionsTest {

    @Test
    void finishedOperationCannotRunAgain() {
        assertThrows(IllegalStateException.class,
            () -> OperationStatus.validateTransition(OperationStatus.SUCCEEDED, OperationStatus.RUNNING));
        assertThrows(IllegalStateException.class,
            () -> OperationStatus.validateTransition(OperationStatus.CANCELED, OperationStatus.FAILED));
    }

    @Test
    void runningOperationCanEndInAnyTerminalStatus() {
        assertDoesNotThrow(() -> OperationStatus.validateTransition(OperationStatus.RUNNING, OperationStatus.SUCCEEDED));
        assertDoesNotThrow(() -> OperationStatus.validateTransition(OperationStatus.RUNNING, OperationStatus.FAILED));
        assertDoesNotThrow(() -> OperationStatus.validateTransition(OperationStatus.RUNNING, OperationStatus.CANCELED));
    }

    @Test
    void onlyTerminalStatusesAreFinished() {
        assertFalse(OperationStatus.PENDING.isFinished());
        assertFalse(OperationStatus.RUNNING.isFinished());
        assertTrue(OperationStatus.SUCCEEDED.isFinished());
        assertTrue(OperationStatus.FAILED.isFinished());
        assertTrue(OperationStatus.CANCELED.isFinished());
    }

    @Test
    void discardedBackupIsFinal() {
        assertThrows(IllegalStateException.class,
            () -> BackupStatus.validateTransition(BackupStatus.DISCARDED, BackupStatus.AVAILABLE));
        assertThrows(IllegalStateException.class,
            () -> BackupStatus.validateTransition(BackupStatus.CREATING, BackupStatus.DISCARDED));
    }

    @Test
    void deviceCanRecoverFromAuthenticationFailure() {
        assertDoesNotThrow(() -> DeviceStatus.validateTransition(DeviceStatus.AUTH_FAILED, DeviceStatus.READY));
        assertDoesNotThrow(() -> DeviceStatus.validateTransition(DeviceStatus.READY, DeviceStatus.READY));
    }
}
