package com.tiksem.pq.data;

import com.tiksem.pq.db.DatabaseManager;

import javax.jdo.InstanceCallbacks;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.Persistent;
import javax.persistence.Entity;
import javax.persistence.Id;;

/**
 * Created by CM on 10/31/2014.
 */
@Entity
public class Photo {
    public static final String IMAGE_URL_PATH = "/image/";

    @Id
    @Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE)
    private Long id;

    @Index
    private Long likesCount;

    @Index
    private Long photoquestId;

    @Index
    private Long userId;

    @NotPersistent
    private String url;

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
