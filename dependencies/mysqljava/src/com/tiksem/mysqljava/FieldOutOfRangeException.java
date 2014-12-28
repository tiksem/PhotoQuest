package com.tiksem.mysqljava;

/**
 * Created by CM on 12/27/2014.
 */
public class FieldOutOfRangeException extends RuntimeException {
    private String fieldName;

    public FieldOutOfRangeException(String fieldName) {
        super(fieldName + " is too large");
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
}
