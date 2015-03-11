package com.tiksem.pq.db;

import java.io.InputStream;

/**
 * Created by CM on 11/29/2014.
 */
public interface ImageManager {
    InputStream getImageById(long id);
    InputStream getImageById(long id, int maxWidth, int maxHeight);
    InputStream getDisplayImage(long id);
    InputStream getThumbnailOfImage(long id, int size);
    void saveImage(long id, InputStream inputStream);
    void deleteImage(long id);
}
