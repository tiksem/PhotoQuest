package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/18/2014.
 */
@Table
@MultipleIndex(fields = {"userId", "photoquestId"}, indexType = IndexType.HASH, isUnique = true)
public class PhotoquestView {
    @Stored
    @NotNull
    private Long userId;
    @Stored
    @NotNull
    private Long photoquestId;
    @AddingDate
    @Stored
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
