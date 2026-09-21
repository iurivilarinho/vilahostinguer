package com.bancada.exception;

/** The device could not be reached or refused the SSH login. Mapped to 502 by the global handler. */
public class DeviceConnectionException extends RuntimeException {

    public DeviceConnectionException(String message) {
        super(message);
    }

    public DeviceConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
