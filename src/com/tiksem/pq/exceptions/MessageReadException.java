package com.tiksem.pq.exceptions;

/**
 * Created by CM on 11/14/2014.
 */
public class MessageReadException extends RuntimeException {
    public MessageReadException() {
    }

    public MessageReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageReadException(String message) {
        super(message);
    }

    public MessageReadException(Throwable cause) {
        super(cause);
    }

    public MessageReadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
