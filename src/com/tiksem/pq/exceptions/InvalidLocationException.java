package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/1/2014.
 */
public class InvalidLocationException extends RuntimeException {
    public InvalidLocationException() {
        super("Invalid location supplied, should be google place id");
    }

    public InvalidLocationException(Throwable cause) {
        super(cause);
    }
}
