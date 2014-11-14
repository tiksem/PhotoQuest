package com.tiksem.pq.data;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.*;

/**
 * Created by CM on 10/31/2014.
 */
@PersistenceCapable
public class Photo implements Likable, InstanceCallbacks {
    public static final String IMAGE_URL_PATH = "/image/";

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Index
    private Long likesCount;

    @Index
    private Long photoquestId;

    @Index
    private Long userId;

    @NotPersistent
    private String url;

    @NotPersistent
    private Like yourLike;

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
}
