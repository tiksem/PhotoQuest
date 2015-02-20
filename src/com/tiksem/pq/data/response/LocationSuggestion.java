package com.tiksem.pq.data.response;

import com.tiksem.mysqljava.annotations.Stored;

/**
 * Created by CM on 2/11/2015.
 */
public class LocationSuggestion {
    @Stored
    private String value;
    @Stored
    private int id;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
