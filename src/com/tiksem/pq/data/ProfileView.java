package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 12/18/2014.
 */

@PersistenceCapable
public class ProfileView {
    @Index
    private Long userId;
    @Index
    private Long visitorId;
    @AddingDate
    @Index
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
