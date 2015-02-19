package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/3/2014.
 */
@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "addingDate"}),
        @MultipleIndex(fields = {"id", "type"})
})
public class Reply {
    public static final int FRIEND_REQUEST_ACCEPTED = 0;
    public static final int FRIEND_REQUEST_DECLINED = 1;
    public static final int COMMENT = 2;
    public static final int LIKE = 3;

    @Stored(type = "INT(1)")
    @NotNull
    private Integer type;

    @Stored
    @NotNull
    private Long id;

    @Stored
    @NotNull
    private Long userId;

    @AddingDate
    @Stored
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

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }
}
