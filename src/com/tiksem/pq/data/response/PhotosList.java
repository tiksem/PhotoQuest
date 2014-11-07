package com.tiksem.pq.data.response;

import com.tiksem.pq.data.Photo;

import java.util.Collection;

/**
 * Created by CM on 11/7/2014.
 */
public class PhotosList {
    public Collection<Photo> photos;

    public PhotosList(Collection<Photo> photos) {
        this.photos = photos;
    }
}
