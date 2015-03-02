package com.tiksem.mysqljava.exceptions;

import com.tiksem.mysqljava.FieldsCheckingUtilities;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 19:30
 */
public class NameFieldPatternException extends FieldCheckingException{
    public NameFieldPatternException(String className, String fieldName, String value) {
        super("'" + value + "' of " + className + "." + fieldName + " should match " +
                FieldsCheckingUtilities.CHECK_NAME_FIELD_PATTERN_STRING + ", cause it is marked with NameField " +
                "annotation", fieldName);
    }
}
