package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 10/31/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"photoquestId", "id"}),
        @MultipleIndex(fields = {"photoquestId", "userId", "likesCount", "id"}),
        @MultipleIndex(fields = {"photoquestId", "userId", "isNew", "viewsCount", "id"}),
        @MultipleIndex(fields = {"photoquestId", "userId", "id"}),
        @MultipleIndex(fields = {"photoquestId", "likesCount", "id"}),
        @MultipleIndex(fields = {"photoquestId", "isNew", "viewsCount", "id"}),
        @MultipleIndex(fields = {"userId", "likesCount", "id"}),
        @MultipleIndex(fields = {"userId", "isNew", "viewsCount", "id"}),
        @MultipleIndex(fields = {"isNew", "addingDate"})
})
public class Photo implements Likable {
    public static final String IMAGE_URL_PATH = "/image/";

    @PrimaryKey
    private Long id;

    @NotNull
    @Stored
    private Long likesCount;

    @NotNull
    @JsonIgnore
    @ForeignKey(parent = Photoquest.class, field = "id")
    private Long photoquestId;

    @NotNull
    @ForeignKey(parent = User.class, field = "id")
    private Long userId;

    @ForeignValue(idField = "userId")
    private User user;

    @ForeignValue(idField = "photoquestId")
    private Photoquest photoquest;

    private String url;

    private Like yourLike;

    private boolean showNextPrevButtons = false;

    @Stored
    @AddingDate
    private Long addingDate;

    @Stored
    @NotNull
    private Long viewsCount;

    @Stored(type = "VARCHAR(200)")
    private String message;

    @Stored
    @JsonIgnore
    @NotNull
    private Boolean isNew;

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public void incrementLikesCount() {
        if(likesCount == null){
            likesCount = 1l;
        } else {
            likesCount++;
        }
    }

    @Override
    public void decrementLikesCount() {
        if(likesCount == null){
            likesCount = 0l;
        } else {
            likesCount--;
        }
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

    public Long getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Long viewsCount) {
        this.viewsCount = viewsCount;
    }

    public void incrementViewsCount() {
        if(viewsCount == null){
            viewsCount = 1l;
        } else {
            viewsCount++;
        }
    }

    public boolean isShowNextPrevButtons() {
        return showNextPrevButtons;
    }

    public void setShowNextPrevButtons(boolean showNextPrevButtons) {
        this.showNextPrevButtons = showNextPrevButtons;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Photoquest getPhotoquest() {
        return photoquest;
    }

    public void setPhotoquest(Photoquest photoquest) {
        this.photoquest = photoquest;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    @OnPrepareForStorage
    void onPrepareForStorage() {
        if(viewsCount == null){
            viewsCount = 0l;
        }

        if(likesCount == null){
            likesCount = 0l;
        }
    }
}
