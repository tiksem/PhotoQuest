package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 12/18/2014.
 */
@PersistenceCapable
public class PhotoView {
    @Index
    private Long photoId;
    @Index
    private Long userId;

    @Index
    @AddingDate
    private Long addingDate;

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
