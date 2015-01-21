package com.tiksem.pq.data.response.android;

import com.tiksem.pq.data.User;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by CM on 1/21/2015.
 */
public class MobileUserList {
    public Collection<MobileUser> users;

    public MobileUserList(Collection<User> users) {
        this.users = new ArrayList<MobileUser>(users.size());
        for(User user : users){
            MobileUser androidUser = new MobileUser(user);
            this.users.add(androidUser);
        }
    }
}
