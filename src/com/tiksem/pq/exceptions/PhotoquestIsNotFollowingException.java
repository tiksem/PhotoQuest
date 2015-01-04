package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/4/2014.
 */
public class PhotoquestIsNotFollowingException extends RuntimeException {
    public PhotoquestIsNotFollowingException(long userId, long photoquestId) {
        super("Photoquest " + photoquestId + " is not following by user " + userId);
    }
}
