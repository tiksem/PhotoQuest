package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;
import com.utils.framework.google.places.Language;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by CM on 12/1/2014.
 */
@Table
public class Location {
    @PrimaryKey
    private String id;
    @Stored
    @NotNull
    private String countryCode;

    @Serialized
    @NotNull
    private Map<Language, LocationInfo> info;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setInfo(Language language, LocationInfo info) {
        if(this.info == null){
            this.info = new EnumMap<Language, LocationInfo>(Language.class);
        }

        this.info.put(language, info);
    }

    public LocationInfo getInfo(Language language) {
        return info.get(language);
    }

    public Map<Language, LocationInfo> getInfo() {
        return info;
    }

    public void setInfo(Map<Language, LocationInfo> info) {
        this.info = info;
    }
}
