package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/1/2014.
 */
public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String id) {
        super("Location with id" + id + " not found");
    }
}
