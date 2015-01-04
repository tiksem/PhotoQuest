package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/18/2014.
 */

@Table
@MultipleIndex(fields = {"userId", "visitorId"}, indexType = IndexType.HASH)
public class ProfileView {
    @Stored
    @NotNull
    private Long userId;
    @Stored
    @NotNull
    private Long visitorId;
    @AddingDate
    @Stored
    private Long addingDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(Long visitorId) {
        this.visitorId = visitorId;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
