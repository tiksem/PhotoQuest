package com.tiksem.mysqljava.metadata;

import com.tiksem.mysqljava.annotations.Stored;

/**
 * Created by CM on 12/29/2014.
 */
public class ForeignKeyInfo {
    @Stored
    private String CONSTRAINT_CATALOG;
    @Stored
    private String CONSTRAINT_SCHEMA;
    @Stored
    private String CONSTRAINT_NAME;
    @Stored
    private String TABLE_CATALOG;
    @Stored
    private String TABLE_SCHEMA;
    @Stored
    private String TABLE_NAME;
    @Stored
    private String COLUMN_NAME;
    @Stored
    private String ORDINAL_POSITION;
    @Stored
    private String POSITION_IN_UNIQUE_CONSTRAINT;
    @Stored
    private String REFERENCED_TABLE_SCHEMA;
    @Stored
    private String REFERENCED_TABLE_NAME;
    @Stored
    private String REFERENCED_COLUMN_NAME;
    @Stored
    private String UPDATE_RULE;
    @Stored
    private String DELETE_RULE;

    public String getCONSTRAINT_CATALOG() {
        return CONSTRAINT_CATALOG;
    }

    public void setCONSTRAINT_CATALOG(String CONSTRAINT_CATALOG) {
        this.CONSTRAINT_CATALOG = CONSTRAINT_CATALOG;
    }

    public String getCONSTRAINT_SCHEMA() {
        return CONSTRAINT_SCHEMA;
    }

    public void setCONSTRAINT_SCHEMA(String CONSTRAINT_SCHEMA) {
        this.CONSTRAINT_SCHEMA = CONSTRAINT_SCHEMA;
    }

    public String getCONSTRAINT_NAME() {
        return CONSTRAINT_NAME;
    }

    public void setCONSTRAINT_NAME(String CONSTRAINT_NAME) {
        this.CONSTRAINT_NAME = CONSTRAINT_NAME;
    }

    public String getTABLE_CATALOG() {
        return TABLE_CATALOG;
    }

    public void setTABLE_CATALOG(String TABLE_CATALOG) {
        this.TABLE_CATALOG = TABLE_CATALOG;
    }

    public String getTABLE_SCHEMA() {
        return TABLE_SCHEMA;
    }

    public void setTABLE_SCHEMA(String TABLE_SCHEMA) {
        this.TABLE_SCHEMA = TABLE_SCHEMA;
    }

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public void setTABLE_NAME(String TABLE_NAME) {
        this.TABLE_NAME = TABLE_NAME;
    }

    public String getCOLUMN_NAME() {
        return COLUMN_NAME;
    }

    public void setCOLUMN_NAME(String COLUMN_NAME) {
        this.COLUMN_NAME = COLUMN_NAME;
    }

    public String getORDINAL_POSITION() {
        return ORDINAL_POSITION;
    }

    public void setORDINAL_POSITION(String ORDINAL_POSITION) {
        this.ORDINAL_POSITION = ORDINAL_POSITION;
    }

    public String getPOSITION_IN_UNIQUE_CONSTRAINT() {
        return POSITION_IN_UNIQUE_CONSTRAINT;
    }

    public void setPOSITION_IN_UNIQUE_CONSTRAINT(String POSITION_IN_UNIQUE_CONSTRAINT) {
        this.POSITION_IN_UNIQUE_CONSTRAINT = POSITION_IN_UNIQUE_CONSTRAINT;
    }

    public String getREFERENCED_TABLE_SCHEMA() {
        return REFERENCED_TABLE_SCHEMA;
    }

    public void setREFERENCED_TABLE_SCHEMA(String REFERENCED_TABLE_SCHEMA) {
        this.REFERENCED_TABLE_SCHEMA = REFERENCED_TABLE_SCHEMA;
    }

    public String getREFERENCED_TABLE_NAME() {
        return REFERENCED_TABLE_NAME;
    }

    public void setREFERENCED_TABLE_NAME(String REFERENCED_TABLE_NAME) {
        this.REFERENCED_TABLE_NAME = REFERENCED_TABLE_NAME;
    }

    public String getREFERENCED_COLUMN_NAME() {
        return REFERENCED_COLUMN_NAME;
    }

    public void setREFERENCED_COLUMN_NAME(String REFERENCED_COLUMN_NAME) {
        this.REFERENCED_COLUMN_NAME = REFERENCED_COLUMN_NAME;
    }

    public String getUPDATE_RULE() {
        return UPDATE_RULE;
    }

    public void setUPDATE_RULE(String UPDATE_RULE) {
        this.UPDATE_RULE = UPDATE_RULE;
    }

    public String getDELETE_RULE() {
        return DELETE_RULE;
    }

    public void setDELETE_RULE(String DELETE_RULE) {
        this.DELETE_RULE = DELETE_RULE;
    }
}
