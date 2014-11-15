package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/15/2014.
 */
public class FriendRequestExistsException extends RuntimeException {
    public FriendRequestExistsException() {
        super("Friend request has been already sent");
    }
}
