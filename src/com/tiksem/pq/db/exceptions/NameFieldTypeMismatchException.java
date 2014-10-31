package com.tiksem.pq.db.exceptions;

import com.tiksem.pq.data.annotations.NameField;

import java.lang.reflect.Field;

/**
 * User: Tikhonenko.S
 * Date: 22.04.2014
 * Time: 18:50
 */
public class NameFieldTypeMismatchException extends FieldCheckingException{
    public NameFieldTypeMismatchException(Field field, Class clazz) {
        super("Field '" + field.getName() + "' of the '" + clazz.getName() + "' class should " +
                "be String, cause it is marked with '" + NameField.class.getName() + "' annotation");
    }
}
