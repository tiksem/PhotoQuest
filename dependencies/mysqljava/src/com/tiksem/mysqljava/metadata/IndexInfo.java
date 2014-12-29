package com.tiksem.mysqljava.metadata;

import com.tiksem.mysqljava.annotations.Stored;

/**
 * Created by CM on 12/29/2014.
 */
public class IndexInfo {
    @Stored
    private String TABLE_NAME;
    @Stored
    private String NON_UNIQUE;
    @Stored
    private String INDEX_NAME;
    @Stored
    private String SEQ_IN_INDEX;
    @Stored
    private String COLUMN_NAME;
    @Stored
    private String COLLATION;
    @Stored
    private String CARDINALITY;
    @Stored
    private String SUB_PART;
    @Stored
    private String PACKED;
    @Stored
    private String NULLABLE;
    @Stored
    private String INDEX_TYPE;
    @Stored
    private String COMMENT;
    @Stored
    private String INDEX_COMMENT;

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public void setTABLE_NAME(String TABLE_NAME) {
        this.TABLE_NAME = TABLE_NAME;
    }

    public String getNON_UNIQUE() {
        return NON_UNIQUE;
    }

    public void setNON_UNIQUE(String NON_UNIQUE) {
        this.NON_UNIQUE = NON_UNIQUE;
    }

    public String getINDEX_NAME() {
        return INDEX_NAME;
    }

    public void setINDEX_NAME(String INDEX_NAME) {
        this.INDEX_NAME = INDEX_NAME;
    }

    public String getSEQ_IN_INDEX() {
        return SEQ_IN_INDEX;
    }

    public void setSEQ_IN_INDEX(String SEQ_IN_INDEX) {
        this.SEQ_IN_INDEX = SEQ_IN_INDEX;
    }

    public String getCOLUMN_NAME() {
        return COLUMN_NAME;
    }

    public void setCOLUMN_NAME(String COLUMN_NAME) {
        this.COLUMN_NAME = COLUMN_NAME;
    }

    public String getCOLLATION() {
        return COLLATION;
    }

    public void setCOLLATION(String COLLATION) {
        this.COLLATION = COLLATION;
    }

    public String getCARDINALITY() {
        return CARDINALITY;
    }

    public void setCARDINALITY(String CARDINALITY) {
        this.CARDINALITY = CARDINALITY;
    }

    public String getSUB_PART() {
        return SUB_PART;
    }

    public void setSUB_PART(String SUB_PART) {
        this.SUB_PART = SUB_PART;
    }

    public String getPACKED() {
        return PACKED;
    }

    public void setPACKED(String PACKED) {
        this.PACKED = PACKED;
    }

    public String getNULLABLE() {
        return NULLABLE;
    }

    public void setNULLABLE(String NULLABLE) {
        this.NULLABLE = NULLABLE;
    }

    public String getINDEX_TYPE() {
        return INDEX_TYPE;
    }

    public void setINDEX_TYPE(String INDEX_TYPE) {
        this.INDEX_TYPE = INDEX_TYPE;
    }

    public String getCOMMENT() {
        return COMMENT;
    }

    public void setCOMMENT(String COMMENT) {
        this.COMMENT = COMMENT;
    }

    public String getINDEX_COMMENT() {
        return INDEX_COMMENT;
    }

    public void setINDEX_COMMENT(String INDEX_COMMENT) {
        this.INDEX_COMMENT = INDEX_COMMENT;
    }
}
