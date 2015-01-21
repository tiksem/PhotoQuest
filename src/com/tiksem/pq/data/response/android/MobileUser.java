package com.tiksem.pq.data.response.android;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tiksem.pq.data.User;

/**
 * Created by CM on 1/21/2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileUser {
    public Long id;
    public Long avatarId;
    public String name;
    public String lastName;
    public String country;
    public String city;

    public MobileUser(User user) {
        this.avatarId = user.getAvatarId();
        this.city = user.getCity();
        this.country = user.getCountry();
        this.id = user.getId();
        this.name = user.getName();
        this.lastName = user.getLastName();
    }
}
