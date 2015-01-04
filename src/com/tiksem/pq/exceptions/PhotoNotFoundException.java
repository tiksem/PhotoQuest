package com.tiksem.pq.exceptions;

/**
 * Created by CM on 11/10/2014.
 */
public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(long id) {
        super("Photo with id " + id + " does not exist");
    }

    public PhotoNotFoundException(String message) {
        super(message);
    }
}
