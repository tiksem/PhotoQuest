package com.tiksem.mysqljava.exceptions;

import java.sql.SQLException;

/**
 * Created by CM on 1/3/2015.
 */
public class ObjectReplacementFailed extends SQLException {
    private final Object object;

    public ObjectReplacementFailed(Object object) {
        super("Replace operation failed " + object);
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
}
