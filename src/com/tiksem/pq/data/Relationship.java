package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Created by CM on 12/5/2014.
 */
@PersistenceCapable
public class Relationship {
    public static final int FRIENDSHIP = 0;
    public static final int FRIEND_REQUEST = 1;
    public static final int FOLLOWS = 2;

    @Index
    private Long fromUserId;
    @Index
    private Long toUserId;
    @Index
    private Integer type;
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
