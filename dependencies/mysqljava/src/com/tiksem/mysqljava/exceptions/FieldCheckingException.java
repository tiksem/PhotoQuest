package com.tiksem.mysqljava.exceptions;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 18:49
 */
public class FieldCheckingException extends RuntimeException{
    private String fieldName;

    public FieldCheckingException(String fieldName) {
        this.fieldName = fieldName;
    }

    public FieldCheckingException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
