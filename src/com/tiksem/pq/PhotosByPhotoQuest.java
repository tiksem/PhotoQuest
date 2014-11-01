package com.tiksem.pq;

import javax.jdo.annotations.Index;

/**
 * Created by CM on 10/31/2014.
 */
public class PhotosByPhotoQuest {
    @Index
    private Long photoQuestId;
    @Index
    private Long photoId;

    public Long getPhotoQuestId() {
        return photoQuestId;
    }

    public void setPhotoQuestId(Long photoQuestId) {
        this.photoQuestId = photoQuestId;
    }

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }
}
