package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/4/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "photoquestId"}, indexType = IndexType.HASH),
        @MultipleIndex(fields = {"userId", "addingDate"})
})
public class FollowingPhotoquest {
    @ForeignKey(parent = Photoquest.class, field = "id")
    @NotNull
    private Long photoquestId;

    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long userId;

    @Stored
    @AddingDate
    private Long addingDate;

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
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
