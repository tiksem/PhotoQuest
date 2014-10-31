package com.tiksem.pq.db.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 18:49
 */
public class FieldCheckingException extends RuntimeException{
    public FieldCheckingException() {
    }

    public FieldCheckingException(String message) {
        super(message);
    }

    public FieldCheckingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FieldCheckingException(Throwable cause) {
        super(cause);
    }

    public FieldCheckingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
