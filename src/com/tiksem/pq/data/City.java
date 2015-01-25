package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.ForeignKey;
import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.pq.data.Country;

/**
 * Created by CM on 1/22/2015.
 */
public class City {
    @PrimaryKey
    private Integer id;
    @Stored
    private String engName;
    @Stored
    private String rusName;
    @ForeignKey(parent = Country.class, field = "id")
    private Short countryId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEngName() {
        return engName;
    }

    public void setEngName(String engName) {
        this.engName = engName;
    }

    public String getRusName() {
        return rusName;
    }

    public void setRusName(String rusName) {
        this.rusName = rusName;
    }

    public Short getCountryId() {
        return countryId;
    }

    public void setCountryId(Short countryId) {
        this.countryId = countryId;
    }
}
