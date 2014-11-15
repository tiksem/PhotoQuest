package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 11/15/2014.
 */
public class FriendRequestNotFoundException extends RuntimeException {
    public FriendRequestNotFoundException() {
        super("Unable to find friend request");
    }
}
