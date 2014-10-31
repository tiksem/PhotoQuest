package com.tiksem.pq.db.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 28.04.2014
 * Time: 13:40
 */
public class UnknownUserException extends RuntimeException{
    public UnknownUserException(String login) {
        super("Could not find user with '" + login + "' login");
    }
}
