package com.bancada.exception;

/** The device answered but refused the credential (or none is linked). */
public class DeviceAuthenticationException extends DeviceConnectionException {

    public DeviceAuthenticationException(String message) {
        super(message);
    }

    public DeviceAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
