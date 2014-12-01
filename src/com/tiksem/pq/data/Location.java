package com.tiksem.pq.data;

import com.utils.framework.google.places.Language;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Serialized;
import java.util.EnumMap;
import java.util.Map;

/**
 * Created by CM on 12/1/2014.
 */
@PersistenceCapable
public class Location {
    @PrimaryKey
    private String id;
    @Index
    private String countryCode;

    @Serialized
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
}
