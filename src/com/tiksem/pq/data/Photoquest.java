package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 10/31/2014.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"userId", "likesCount"}),
        @MultipleIndex(fields = {"userId", "viewsCount"})
})
public class Photoquest implements WithAvatar, Likable {
    @PrimaryKey
    private Long id;

    @Unique(type = "VARCHAR(30)", indexType = IndexType.HASH)
    @NotNull
    private String name;

    @Stored
    @NotNull
    private Long likesCount;

    @Stored
    @NotNull
    private Long viewsCount;

    @ForeignKey(parent = User.class, field = "id")
    private Long userId;
    @Stored
    private Long avatarId;

    @Stored
    @AddingDate
    private Long addingDate;

    private String avatar;

    private Boolean isFollowing;

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

    public Long getAddingDate() {
        return addingDate;
    }

    public void setAddingDate(Long addingDate) {
        this.addingDate = addingDate;
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

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }
}
