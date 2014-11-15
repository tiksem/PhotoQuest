package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.*;

/**
 * Created by CM on 11/15/2014.
 */
@PersistenceCapable
public class FriendRequest {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;
    @Index
    private Long fromUserId;
    @Index
    private Long toUserId;
    @AddingDate
    @Index
    private Long addingDate;

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
