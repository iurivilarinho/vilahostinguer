package com.bancada.exception;

/** No valid customer session: answered with 401 so the panel sends the visitor to the login. */
public class PortalAuthenticationException extends RuntimeException {

    public PortalAuthenticationException(String message) {
        super(message);
    }
}
