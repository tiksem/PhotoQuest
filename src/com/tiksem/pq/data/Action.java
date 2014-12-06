package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 12/6/2014.
 */
@PersistenceCapable
public class Action {
    @Index
    private Long userId;
    @Index
    private Long photoquestId;
    @Index
    private Long photoId;
    @Index
    @AddingDate
    private Long addingDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
    }

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
