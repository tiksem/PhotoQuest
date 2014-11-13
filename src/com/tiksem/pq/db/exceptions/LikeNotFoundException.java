package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/11/2014.
 */
public class LikeNotFoundException extends RuntimeException {
    public LikeNotFoundException(long id) {
        super("Like with id " + id + " does not exist");
    }
}
