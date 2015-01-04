package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 11/14/2014.
 */
@Table
@MultipleIndex(fields = {"dialogId", "id"})
public class Message {
    @PrimaryKey
    private Long id;

    @Index(indexType = IndexType.HASH)
    private Long dialogId;

    @Stored
    private Long fromUserId;
    @Stored
    private Long toUserId;

    @Stored
    private String message;

    @Stored
    private Boolean read;

    @Stored
    @AddingDate
    private Long addingDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    @OnPrepareForStorage
    private void prepareForStorage() {
        if (read == null) {
            read = false;
        }
    }

    public Long getDialogId() {
        return dialogId;
    }

    public void setDialogId(Long dialogId) {
        this.dialogId = dialogId;
    }
}
