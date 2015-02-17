package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.Photo;
import com.utils.framework.CollectionUtils;

import java.util.Collection;

/**
 * Created by CM on 2/18/2015.
 */
public class MobilePhotoList {
    public Collection<MobilePhoto> photos;

    public MobilePhotoList(Collection<Photo> photos) {
        this.photos = CollectionUtils.transform(photos, new CollectionUtils.Transformer<Photo, MobilePhoto>() {
            @Override
            public MobilePhoto get(Photo photo) {
                return new MobilePhoto(photo);
            }
        });
    }
}
