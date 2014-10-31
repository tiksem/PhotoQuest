package com.tiksem.pq.db.exceptions;

/**
 * Created by CM on 10/31/2014.
 */
public class PhotoquestExistsException extends RuntimeException {
    public PhotoquestExistsException(String photoquestName) {
        super("Photoquest with " + photoquestName + " name already exists");
    }
}
