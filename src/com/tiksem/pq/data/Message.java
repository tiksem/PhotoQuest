package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;
import com.tiksem.pq.data.annotations.OnPrepareForStorage;
import com.tiksem.pq.data.annotations.Relation;

import javax.jdo.annotations.*;

/**
 * Created by CM on 11/14/2014.
 */
@PersistenceCapable
@PersistenceAware
public class Message {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Index
    @Relation(relationName = "user")
    private Long fromUserId;
    @Index
    @Relation(relationName = "user")
    private Long toUserId;

    @Persistent
    private String message;

    @Index
    @AddingDate
    private Long addingDate;

    @Index
    private Boolean read;

    @Index
    private Boolean deletedBySender;

    @Index
    private Boolean deletedByReceiver;

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

    public Boolean getDeletedBySender() {
        return deletedBySender;
    }

    public void setDeletedBySender(Boolean deletedBySender) {
        this.deletedBySender = deletedBySender;
    }

    public Boolean getDeletedByReceiver() {
        return deletedByReceiver;
    }

    public void setDeletedByReceiver(Boolean deletedByReceiver) {
        this.deletedByReceiver = deletedByReceiver;
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
        if (deletedByReceiver == null) {
            deletedByReceiver = false;
        }
        if (deletedBySender == null) {
            deletedBySender = false;
        }
    }
}
