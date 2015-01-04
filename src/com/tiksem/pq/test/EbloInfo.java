package com.tiksem.pq.test;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/27/2014.
 */
public class EbloInfo {
    @ForeignKey(parent = Eblo.class, field = "id")
    @Index
    private Long id;
    @Stored(type = "TEXT")
    private String info;

    @ForeignValue(idField = "id")
    private Eblo eblo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Eblo getEblo() {
        return eblo;
    }

    public void setEblo(Eblo eblo) {
        this.eblo = eblo;
    }
}
