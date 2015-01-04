package com.tiksem.pq.exceptions;

/**
 * Created by CM on 11/9/2014.
 */
public class FriendshipNotExistsException extends RuntimeException {
    public FriendshipNotExistsException(long user1, long user2) {
        super("Friendship between user " + user1 + " and user " + user2 +
                " does not exist");
    }
}
