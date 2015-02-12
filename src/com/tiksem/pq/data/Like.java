package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 11/10/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "photoId"}, isUnique = true),
        @MultipleIndex(fields = {"userId", "commentId"}, isUnique = true)
})
public class Like implements WithPhoto {
    @PrimaryKey
    private Long id;

    @Index
    private Long photoId;
    @Index
    private Long commentId;

    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    private Long userId;

    @ForeignValue(idField = "userId")
    private User user;

    private String photo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
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

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
