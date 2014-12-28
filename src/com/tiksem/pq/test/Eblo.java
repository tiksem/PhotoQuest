package com.tiksem.pq.test;

import com.tiksem.mysqljava.annotations.Index;
import com.tiksem.mysqljava.annotations.PrimaryKey;
import com.tiksem.mysqljava.annotations.Stored;
import com.tiksem.mysqljava.annotations.Unique;

/**
 * Created by CM on 12/27/2014.
 */
public class Eblo {
    @PrimaryKey
    private Long id;
    @Index
    private String name;
    @Unique
    private String phoneNumber;
    @Stored
    private Boolean stored;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getStored() {
        return stored;
    }

    public void setStored(Boolean stored) {
        this.stored = stored;
    }
}
