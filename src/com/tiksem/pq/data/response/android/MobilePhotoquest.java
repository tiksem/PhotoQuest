package com.tiksem.pq.data.response.android;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiksem.pq.data.Photoquest;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 1/21/2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobilePhotoquest {
    public String name;
    public Long avatarId;
    public String createdBy;
    public Long viewsCount;

    public MobilePhotoquest(Photoquest photoquest) {
        name = photoquest.getName();
        avatarId = photoquest.getAvatarId();
        User user = photoquest.getUser();
        createdBy = user.getName() + " " + user.getLastName();
        viewsCount = photoquest.getViewsCount();
    }
}
