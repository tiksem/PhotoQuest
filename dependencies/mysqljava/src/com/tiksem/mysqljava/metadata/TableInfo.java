package com.tiksem.mysqljava.metadata;

import com.tiksem.mysqljava.annotations.Stored;

/**
 * Created by CM on 12/27/2014.
 */
public class TableInfo {
    @Stored
    private String field;
    @Stored
    private String type;
    @Stored
    private boolean isNull;
}
