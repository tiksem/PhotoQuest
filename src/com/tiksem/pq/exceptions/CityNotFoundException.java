package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/1/2014.
 */
public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String id) {
        super("Location with id" + id + " not found");
    }
}
