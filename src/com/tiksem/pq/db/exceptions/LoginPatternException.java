package com.tiksem.pq.db.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 16:41
 */
public class LoginPatternException extends RegisterFailedException{
    public LoginPatternException() {
        super("Username should have at least 2 characters, " +
                "starts with _ or latin letter and contains only _, digits or latin letters");
    }
}
