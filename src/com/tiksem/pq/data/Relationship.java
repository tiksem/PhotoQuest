package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/5/2014.
 */
@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"fromUserId", "toUserId"}, indexType = IndexType.HASH),
        @MultipleIndex(fields = {"fromUserId", "type", "addingDate"}, indexType = IndexType.BTREE),
        @MultipleIndex(fields = {"toUserId", "type", "addingDate"}, indexType = IndexType.BTREE)
})
public class Relationship {
    public static final int FRIENDSHIP = 0;
    public static final int FRIEND_REQUEST = 1;
    public static final int FOLLOWS = 2;

    @Stored
    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long fromUserId;
    @Index(indexType = IndexType.HASH)
    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long toUserId;
    @Stored
    @NotNull
    private Integer type;
    @AddingDate
    @Stored
    private Long addingDate;

    @ForeignValue(idField = "fromUserId")
    private User fromUser;
    @ForeignValue(idField = "toUserId")
    private User toUser;

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

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }
}
