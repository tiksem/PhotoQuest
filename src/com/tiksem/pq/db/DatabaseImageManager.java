package com.tiksem.pq.db;

import com.tiksem.pq.data.BitmapData;
import org.apache.commons.io.IOUtils;

import javax.jdo.PersistenceManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by CM on 11/29/2014.
 */
public class DatabaseImageManager implements ImageManager {
    private PersistenceManager persistenceManager;

    public DatabaseImageManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public InputStream getImageById(long id) {
        BitmapData bitmapData =
                DBUtilities.getObjectById(persistenceManager, BitmapData.class, id);
        if(bitmapData == null){
            return null;
        }

        return new ByteArrayInputStream(bitmapData.getImage());
    }

    @Override
    public void saveImage(long id, InputStream inputStream) {
        BitmapData data = new BitmapData();
        data.setId(id);
        try {
            data.setImage(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DBUtilities.makePersistent(persistenceManager, data);
    }

    @Override
    public InputStream getThumbnailOfImage(long id, int size) {
        return getImageById(id);
    }
}
