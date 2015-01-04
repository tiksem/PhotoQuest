package com.tiksem.pq.exceptions;

/**
 * Created by CM on 11/8/2014.
 */
public class FriendshipExistsException extends RuntimeException {
    public FriendshipExistsException(long user1, long user2) {
        super("Friendship between user " + user1 + " and user " + user2 +
                " already exists");
    }
}
