package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/14/2014.
 */
public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(long id) {
        super("Message with id " + id + " doesn't exist");
    }
}
