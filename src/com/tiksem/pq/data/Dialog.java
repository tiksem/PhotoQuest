package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.pq.data.annotations.OnPrepareForStorage;
import com.tiksem.pq.data.annotations.Relation;

import javax.jdo.annotations.*;

/**
 * Created by CM on 11/15/2014.
 */
@PersistenceCapable
public class Dialog {
    @Index
    @JsonIgnore
    @Relation(relationName = "user")
    private Long user1;
    @Index
    @JsonIgnore
    @Relation(relationName = "user")
    private Long user2;

    @Index
    private Long lastMessageId;

    @Index
    private Long lastMessageTime;

    @NotPersistent
    private Message lastMessage;

    @NotPersistent
    private User user;

    public Long getUser1() {
        return user1;
    }

    public void setUser1(Long user1) {
        this.user1 = user1;
    }

    public Long getUser2() {
        return user2;
    }

    public void setUser2(Long user2) {
        this.user2 = user2;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
