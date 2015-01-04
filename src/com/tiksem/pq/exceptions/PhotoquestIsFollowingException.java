package com.tiksem.pq.exceptions;

/**
 * Created by CM on 12/4/2014.
 */
public class PhotoquestIsFollowingException extends RuntimeException {
    public PhotoquestIsFollowingException() {
        super("Photoquest is already following");
    }
}
