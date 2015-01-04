package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 11/15/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"user1Id", "user2Id", "lastMessageTime"}),
})
public class Dialog {
    @NotNull
    @Index(indexType = IndexType.HASH)
    private Long id;

    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long user1Id;
    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long user2Id;

    @ForeignKey(parent = Message.class, field = "id")
    @NotNull
    private Long lastMessageId;

    @Stored
    @NotNull
    private Long lastMessageTime;

    @ForeignValue(idField = "lastMessageId")
    private Message lastMessage;

    @JsonIgnore
    @ForeignValue(idField = "user1Id")
    private User user1;
    @JsonIgnore
    @ForeignValue(idField = "user2Id")
    private User user2;

    private User user;

    public Long getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
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

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
