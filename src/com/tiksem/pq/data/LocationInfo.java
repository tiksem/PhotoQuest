package com.tiksem.pq.data;

import java.io.Serializable;

/**
 * Created by CM on 12/1/2014.
 */
public class LocationInfo implements Serializable {
    private String city;
    private String country;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
