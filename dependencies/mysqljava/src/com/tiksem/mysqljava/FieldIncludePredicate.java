package com.tiksem.mysqljava;

/**
 * Created by CM on 3/13/2015.
 */
public interface FieldIncludePredicate {
    boolean shouldIncludeField(Class aClass, String fieldName);
}
