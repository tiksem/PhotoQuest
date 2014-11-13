package com.tiksem.pq.data;

import com.tiksem.pq.data.response.Likable;

import javax.jdo.annotations.*;
import javax.persistence.Transient;

/**
 * Created by CM on 11/10/2014.
 */
@PersistenceCapable
public class Comment implements Likable {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;
    private String message;
    @Index
    private Long photoId;
    @Index
    private Long userId;
    @Persistent
    private Long toUserId;
    @Persistent
    private long likesCount = 0;

    @NotPersistent
    private User user;
    @NotPersistent
    private User toUser;
    @Persistent
    private Long toCommentId;

    @NotPersistent
    private Like yourLike;

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

    public long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(long likesCount) {
        this.likesCount = likesCount;
    }

    @Override
    public void incrementLikesCount() {
        likesCount++;
    }

    @Override
    public void decrementLikesCount() {
        likesCount--;
    }

    public Like getYourLike() {
        return yourLike;
    }

    public void setYourLike(Like yourLike) {
        this.yourLike = yourLike;
    }
}
