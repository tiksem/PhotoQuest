package com.tiksem.pq.exceptions;

import com.tiksem.mysqljava.exceptions.RegisterFailedException;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 16:40
 */
public class UserExistsRegisterException extends RegisterFailedException {
    private String userName;

    public UserExistsRegisterException(String userName) {
        super("Username '" + userName + "' is taken");
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }
}
