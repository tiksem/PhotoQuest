package com.tiksem.pq.data.response;

import com.tiksem.pq.data.User;

import java.util.Collection;

/**
 * Created by CM on 11/8/2014.
 */
public class UsersList {
    public Collection<User> users;

    public UsersList(Collection<User> users) {
        this.users = users;
    }
}
