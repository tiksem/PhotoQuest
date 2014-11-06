package com.tiksem.pq.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.*;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by CM on 10/31/2014.
 */

@Entity
public class Photoquest implements InstanceCallbacks {
    @Id
    @Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE)
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

    public Long getAvatarId() {
        return avatarId;
    }

    public void setAvatarId(Long avatarId) {
        this.avatarId = avatarId;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public void jdoPreClear() {

    }

    @Override
    public void jdoPreDelete() {

    }

    @Override
    public void jdoPostLoad() {
        if(avatarId != null){
            avatar = Photo.IMAGE_URL_PATH + avatarId;
        }
    }

    @Override
    public void jdoPreStore() {

    }
}
