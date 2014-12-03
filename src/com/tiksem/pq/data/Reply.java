package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.pq.data.annotations.AddingDate;

import javax.jdo.annotations.Index;

/**
 * Created by CM on 12/3/2014.
 */
public class Reply {
    public static final int FRIEND_REQUEST_ACCEPTED = 0;
    public static final int FRIEND_REQUEST_DECLINED = 1;
    public static final int COMMENT = 2;

    @Index
    private Integer type;
    @Index
    private Long id;
    @Index
    private Long userId;

    @AddingDate
    @Index
    private Long addingDate;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
