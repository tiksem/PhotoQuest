package com.tiksem.pq.db;

import com.tiksem.pq.data.BitmapData;

import javax.jdo.PersistenceManager;

/**
 * Created by CM on 11/29/2014.
 */
public class DatabaseImageManager implements ImageManager {
    private PersistenceManager persistenceManager;

    public DatabaseImageManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public byte[] getImageById(long id) {
        BitmapData bitmapData =
                DBUtilities.getObjectById(persistenceManager, BitmapData.class, id);
        if(bitmapData == null){
            return null;
        }

        return bitmapData.getImage();
    }

    @Override
    public void saveImage(long id, byte[] value) {
        BitmapData data = new BitmapData();
        data.setId(id);
        data.setImage(value);
        DBUtilities.makePersistent(persistenceManager, data);
    }
}
