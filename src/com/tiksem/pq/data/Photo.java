package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.AddingDate;
import com.tiksem.pq.data.annotations.NotNull;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.*;

/**
 * Created by CM on 10/31/2014.
 */
@PersistenceCapable
@PersistenceAware
public class Photo implements Likable, InstanceCallbacks {
    public static final String IMAGE_URL_PATH = "/image/";

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Index
    private Long likesCount;

    @NotNull
    @Index
    private Long photoquestId;

    @NotNull
    @Index
    private Long userId;

    @NotPersistent
    private String url;

    @NotPersistent
    private Like yourLike;

    @NotPersistent
    private Long position;

    @NotPersistent
    private boolean showNextPrevButtons = false;

    @Index
    @AddingDate
    private Long addingDate;

    @Index
    private Long viewsCount;

    @Persistent
    private String message;

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

    @Override
    public void jdoPreClear() {

    }

    @Override
    public void jdoPreDelete() {

    }

    @Override
    public void jdoPostLoad() {
        if(likesCount == null){
            likesCount = 0l;
        }
    }

    @Override
    public void jdoPreStore() {

    }

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
    }

    public Long getPosition() {
        return position;
    }

    public void setPosition(Long position) {
        this.position = position;
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
}
