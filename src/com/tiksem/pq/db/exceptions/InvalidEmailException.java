package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/14/2014.
 */
public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String email) {
        super(email + " email is invalid");
    }
}
