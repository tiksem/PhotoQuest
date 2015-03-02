package com.tiksem.pq.exceptions;

import com.tiksem.mysqljava.FieldsCheckingUtilities;
import com.tiksem.mysqljava.exceptions.FieldCheckingException;

/**
 * User: Tikhonenko.S
 * Date: 06.05.2014
 * Time: 19:14
 */
public class SentenceFieldPatternException extends FieldCheckingException {
    public SentenceFieldPatternException(String className, String fieldName, String value) {
        super("'" + value + "' of " + className + "." + fieldName + " should match " +
                FieldsCheckingUtilities.CHECK_SENTENCE_FIELD_PATTERN_STRING +
                ", cause it is marked with SentenceField " +
                "annotation", fieldName);
    }
}
