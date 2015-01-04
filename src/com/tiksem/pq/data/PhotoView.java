package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/18/2014.
 */
@Table
@MultipleIndex(fields = {"userId", "photoId"}, indexType = IndexType.HASH)
public class PhotoView {
    @Stored
    @NotNull
    private Long photoId;
    @Stored
    @NotNull
    private Long userId;

    @Stored
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
