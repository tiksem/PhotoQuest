package com.tiksem.pq.data;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Persistent;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by CM on 11/10/2014.
 */
@Entity
public class Comment {
    @Id
    @Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE)
    private Long id;
    private String message;
    @Index
    private Long photoId;
    @Index
    private Long userId;
    @Persistent
    private Long toUserId;

    @NotPersistent
    private User user;
    @NotPersistent
    private User toUser;
    @Persistent
    private Long toCommentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public Long getToCommentId() {
        return toCommentId;
    }

    public void setToCommentId(Long toCommentId) {
        this.toCommentId = toCommentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }
}
