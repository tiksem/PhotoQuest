package com.tiksem.pq.db.exceptions;

import com.tiksem.pq.db.FieldsCheckingUtilities;

/**
 * User: Tikhonenko.S
 * Date: 25.04.2014
 * Time: 16:46
 */
public class PasswordPatternException extends RegisterFailedException{
    public PasswordPatternException() {
        super("Password should match " + FieldsCheckingUtilities.CHECK_PASSWORD_FIELD_PATTERN_STRING + " pattern");
    }
}
