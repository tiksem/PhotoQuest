package com.tiksem.pq.data;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Unique;
import javax.persistence.Entity;

/**
 * Created by CM on 11/6/2014.
 */
@Entity
public class BitmapData {
    @Index
    @Unique
    private Long id;
    private byte[] image;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
