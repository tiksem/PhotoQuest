package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Table;
import com.tiksem.mysqljava.annotations.Unique;

/**
 * Created by CM on 2/8/2015.
 */
@Table
public class Country {
    @PrimaryKey
    private Integer id;

    @Unique
    private String ruName;
    @Unique
    private String enName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRuName() {
        return ruName;
    }

    public void setRuName(String ruName) {
        this.ruName = ruName;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }
}
