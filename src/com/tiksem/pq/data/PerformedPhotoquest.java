package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceAware;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 11/15/2014.
 */
@PersistenceCapable
@PersistenceAware
public class PerformedPhotoquest {
    @Index
    private Long userId;
    @Index
    private Long photoquestId;

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

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}