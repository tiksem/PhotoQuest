package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 11/10/2014.
 */

@Table
public class Comment implements Likable, WithPhoto {
    @PrimaryKey
    private Long id;

    @Stored
    @NotNull
    private String message;

    @ForeignKey(parent = Photo.class, field = "id")
    @NotNull
    private Long photoId;

    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long userId;

    @ForeignKey(parent = User.class, field = "id")
    private Long toUserId;

    @Stored
    @NotNull
    private Long likesCount;

    @ForeignKey(parent = Comment.class, field = "id", onDelete = OnDelete.SET_NULL)
    private Long toCommentId;

    @ForeignValue(idField = "userId")
    private User user;
    @ForeignValue(idField = "toUserId")
    private User toUser;

    private Like yourLike;

    private String photo;

    @Stored
    @AddingDate
    private Long addingDate;

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

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
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

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @OnPrepareForStorage
    void prepareForStorage(){
        if(likesCount == null){
            likesCount = 0l;
        }
    }
}
