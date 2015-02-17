package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Photo;

/**
 * Created by CM on 2/18/2015.
 */
public class MobilePhoto {
    public long id;

    public MobilePhoto(Photo photo) {
        id = photo.getId();
    }
}
