package com.bancada.exception;

/** Too many wrong passwords in a short time: answered with 429. */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
