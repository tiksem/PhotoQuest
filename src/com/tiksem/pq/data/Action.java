package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/6/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "id"})
})
public class Action {
    @PrimaryKey
    @JsonIgnore
    private Long id;

    @ForeignKey(parent = User.class, field = "id")
    @NotNull
    @JsonIgnore
    private Long userId;

    @ForeignKey(parent = Photoquest.class, field = "id")
    @JsonIgnore
    private Long photoquestId;

    @ForeignKey(parent = Photo.class, field = "id")
    @JsonIgnore
    private Long photoId;

    @Stored
    @AddingDate
    private Long addingDate;

    @ForeignValue(idField = "photoquestId")
    private Photoquest photoquest;
    @ForeignValue(idField = "userId")
    private User user;
    @ForeignValue(idField = "photoId")
    private Photo photo;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
    }

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Photoquest getPhotoquest() {
        return photoquest;
    }

    public void setPhotoquest(Photoquest photoquest) {
        this.photoquest = photoquest;
    }

    public Photo getPhoto() {
        return photo;
    }

    public void setPhoto(Photo photo) {
        this.photo = photo;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
