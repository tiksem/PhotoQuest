package com.tiksem.mysqljava.security;

/**
 * Created by CM on 2/13/2015.
 */
public class IpSecurityException extends RuntimeException {
    public IpSecurityException(String message) {
        super(message);
    }

    public IpSecurityException() {
    }

    public IpSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public IpSecurityException(Throwable cause) {
        super(cause);
    }

    public IpSecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
