package com.tiksem.pq.db.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 17:39
 */
public class UserIsNotSignInException extends RuntimeException{
    public UserIsNotSignInException() {
        super("Sign in, please!");
    }
}
