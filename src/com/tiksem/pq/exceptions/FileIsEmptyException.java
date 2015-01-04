package com.tiksem.pq.exceptions;

/**
 * Created by CM on 10/31/2014.
 */
public class FileIsEmptyException extends RuntimeException {
    public FileIsEmptyException() {
    }

    public FileIsEmptyException(String message) {
        super(message);
    }

    public FileIsEmptyException(Throwable cause) {
        super(cause);
    }

    public FileIsEmptyException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileIsEmptyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
