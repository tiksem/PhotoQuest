package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.mysqljava.annotations.Table;

/**
 * Created by CM on 1/22/2015.
 */
@Table
public class Country {
    @PrimaryKey(autoincrement = false)
    private Short id;

    @Stored
    private String rusName;
    @Stored
    private String engName;

    public Short getId() {
        return id;
    }

    public void setId(Short id) {
        this.id = id;
    }

    public String getRusName() {
        return rusName;
    }

    public void setRusName(String rusName) {
        this.rusName = rusName;
    }

    public String getEngName() {
        return engName;
    }

    public void setEngName(String engName) {
        this.engName = engName;
    }
}
