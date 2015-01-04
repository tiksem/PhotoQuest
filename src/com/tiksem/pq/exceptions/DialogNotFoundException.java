package com.tiksem.pq.exceptions;

/**
 * Created by CM on 1/1/2015.
 */
public class DialogNotFoundException extends RuntimeException {
    public DialogNotFoundException(long id) {
        super("Dialog with id " + id + " does not exist");
    }
}
