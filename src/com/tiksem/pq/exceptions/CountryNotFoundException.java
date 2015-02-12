package com.tiksem.pq.exceptions;

/**
 * Created by CM on 2/11/2015.
 */
public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(Integer id) {
        super("Country with id " + id + " does not exist");
    }

    public CountryNotFoundException(String name) {
        super("Country with name " + name + " does not exist");
    }
}
