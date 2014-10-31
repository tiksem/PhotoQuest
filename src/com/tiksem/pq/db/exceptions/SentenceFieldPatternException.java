package com.tiksem.pq.db.exceptions;

import com.tiksem.pq.db.FieldsCheckingUtilities;

/**
 * User: Tikhonenko.S
 * Date: 06.05.2014
 * Time: 19:14
 */
public class SentenceFieldPatternException extends FieldCheckingException{
    public SentenceFieldPatternException(String className, String fieldName, String value) {
        super("'" + value + "' of " + className + "." + fieldName + " should match " +
                FieldsCheckingUtilities.CHECK_SENTENCE_FIELD_PATTERN_STRING +
                ", cause it is marked with SentenceField " +
                "annotation");
    }
}
