package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/15/2014.
 */
public class RelationNotFoundException extends RuntimeException {
    public RelationNotFoundException() {
        super("Unable to find friend request");
    }
}
