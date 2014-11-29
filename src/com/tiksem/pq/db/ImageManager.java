package com.tiksem.pq.db;

/**
 * Created by CM on 11/29/2014.
 */
public interface ImageManager {
    byte[] getImageById(long id);
    void saveImage(long id, byte[] value);
}
