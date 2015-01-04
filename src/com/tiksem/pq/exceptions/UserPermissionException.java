package com.tiksem.pq.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 29.04.2014
 * Time: 14:11
 */
public class UserPermissionException extends RuntimeException {
    public UserPermissionException() {
        super("Access denied, the user does not have permissions to perform this operation");
    }
}
