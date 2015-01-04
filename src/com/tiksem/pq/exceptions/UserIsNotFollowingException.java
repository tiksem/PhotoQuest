package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/5/2014.
 */
public class UserIsNotFollowingException extends RuntimeException {
    public UserIsNotFollowingException() {
        super("User is not following!");
    }
}
