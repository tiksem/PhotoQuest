package com.tiksem.pq.data.response.android;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiksem.pq.data.Action;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 1/21/2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileFeed {
    public Long photoId;
    public Long photoquestId;
    public Long userId;
    public Long addingDate;
    public Long likesCount;
    public Long avatarId;
    public String photoquestName;
    public String userName;

    public MobileFeed(Action action) {
        photoId = action.getPhotoId();
        photoquestId = action.getPhotoquestId();
        userId = action.getUserId();
        addingDate = action.getAddingDate();

        if (photoId != null) {
            likesCount = action.getPhoto().getLikesCount();
        }

        User user = action.getUser();
        if (user != null) {
            avatarId = user.getAvatarId();
            userName = user.getName() + " " + user.getLastName();
        }
        photoquestName = action.getPhotoquest().getName();
    }
}
