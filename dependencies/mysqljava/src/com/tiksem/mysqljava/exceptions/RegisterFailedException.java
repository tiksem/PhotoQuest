package com.tiksem.mysqljava.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 16:39
 */
public class RegisterFailedException extends RuntimeException{
    public RegisterFailedException() {
    }

    public RegisterFailedException(String message) {
        super(message);
    }

    public RegisterFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegisterFailedException(Throwable cause) {
        super(cause);
    }

    public RegisterFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
