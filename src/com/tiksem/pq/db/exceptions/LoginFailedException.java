package com.tiksem.pq.db.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 16:24
 */
public class LoginFailedException extends RuntimeException{
    public LoginFailedException() {
        super("Invalid login or password!");
    }
}
