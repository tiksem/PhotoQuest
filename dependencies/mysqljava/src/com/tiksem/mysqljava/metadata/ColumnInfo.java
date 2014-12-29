package com.tiksem.mysqljava.metadata;

import com.tiksem.mysqljava.annotations.Stored;

/**
 * Created by CM on 12/27/2014.
 */
public class ColumnInfo {
    @Stored
    private String COLUMN_NAME;
    @Stored
    private String COLUMN_TYPE;
    @Stored
    private String IS_NULLABLE;
    @Stored
    private String COLUMN_KEY;
    @Stored
    private String COLUMN_DEFAULT;
    @Stored
    private String EXTRA;

    public String getCOLUMN_NAME() {
        return COLUMN_NAME;
    }

    public void setCOLUMN_NAME(String COLUMN_NAME) {
        this.COLUMN_NAME = COLUMN_NAME;
    }

    public String getCOLUMN_TYPE() {
        return COLUMN_TYPE;
    }

    public void setCOLUMN_TYPE(String COLUMN_TYPE) {
        this.COLUMN_TYPE = COLUMN_TYPE;
    }

    public String getCOLUMN_KEY() {
        return COLUMN_KEY;
    }

    public void setCOLUMN_KEY(String COLUMN_KEY) {
        this.COLUMN_KEY = COLUMN_KEY;
    }

    public String getIS_NULLABLE() {
        return IS_NULLABLE;
    }

    public void setIS_NULLABLE(String IS_NULLABLE) {
        this.IS_NULLABLE = IS_NULLABLE;
    }

    public String getCOLUMN_DEFAULT() {
        return COLUMN_DEFAULT;
    }

    public void setCOLUMN_DEFAULT(String COLUMN_DEFAULT) {
        this.COLUMN_DEFAULT = COLUMN_DEFAULT;
    }

    public String getEXTRA() {
        return EXTRA;
    }

    public void setEXTRA(String EXTRA) {
        this.EXTRA = EXTRA;
    }
}
