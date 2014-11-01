package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 10/31/2014.
 */
public class PhotoquestNotFoundException extends RuntimeException {
    public PhotoquestNotFoundException(String photoquestName) {
        super("Photoquest '" + photoquestName + "' does not exist");
    }
}
