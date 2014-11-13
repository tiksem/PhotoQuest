package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.*;
import javax.persistence.Transient;

/**
 * Created by CM on 10/31/2014.
 */

@PersistenceCapable
public class Photoquest implements WithAvatar {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long id;

    @Unique
    @Persistent
    private String name;
    @Index
    private Long likesCount;
    @Index
    private Long viewsCount;
    @Index
    private Long userId;
    @Index
    @JsonIgnore
    private Long avatarId;

    @NotPersistent
    private String avatar;

    public static Photoquest withZeroViewsAndLikes(String name) {
        Photoquest photoquest = new Photoquest();
        photoquest.likesCount = 0l;
        photoquest.viewsCount = 0l;
        photoquest.name = name;
        return photoquest;
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

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getAvatarId() {
        return avatarId;
    }

    @Override
    public void setAvatarId(Long avatarId) {
        this.avatarId = avatarId;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    @Override
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
