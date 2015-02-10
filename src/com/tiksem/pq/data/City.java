package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 2/8/2015.
 */

@Table
@MultipleIndexes(indexes = {
        @MultipleIndex(fields = {"countryId", "ruName"}, isUnique = true),
        @MultipleIndex(fields = {"countryId", "enName"}, isUnique = true)
})
public class City {
    @PrimaryKey
    private Integer id;

    @Stored
    private String ruName;

    @Stored
    private String enName;

    @Index
    private Integer countryId;

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

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }
}
