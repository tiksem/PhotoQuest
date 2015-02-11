package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/1/2014.
 */
public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(Integer id) {
        super("City with id" + id + " not found");
    }
}
