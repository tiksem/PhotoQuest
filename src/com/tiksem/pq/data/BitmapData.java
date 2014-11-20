package com.tiksem.pq.data;

import com.tiksem.pq.data.annotations.NotNull;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Unique;

/**
 * Created by CM on 11/6/2014.
 */
@PersistenceCapable
public class BitmapData {
    @NotNull
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
