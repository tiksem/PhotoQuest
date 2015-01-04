package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 11/15/2014.
 */
@Table
@MultipleIndex(indexType = IndexType.HASH, fields = {"userId", "photoquestId"}, isUnique = true)
public class PerformedPhotoquest {
    @NotNull
    @ForeignKey(parent = User.class, field = "id")
    private Long userId;
    @Stored
    @NotNull
    private Long photoquestId;

    @Stored
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
