package com.tiksem.pq.db;

import java.io.InputStream;

/**
 * Created by CM on 11/29/2014.
 */
public interface ImageManager {
    InputStream getImageById(long id);
    void saveImage(long id, InputStream inputStream);
}
