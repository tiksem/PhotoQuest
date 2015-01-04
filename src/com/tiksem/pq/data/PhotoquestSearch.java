package com.tiksem.pq.data;

import com.tiksem.mysqljava.annotations.*;

/**
 * Created by CM on 12/31/2014.
 */

@Table
public class PhotoquestSearch {
    @ForeignKey(parent = Photoquest.class, field = "id", unique = true)
    @NotNull
    private Long photoquestId;

    @Index(indexType = IndexType.FULLTEXT)
    @NotNull
    private String keywords;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Long getPhotoquestId() {
        return photoquestId;
    }

    public void setPhotoquestId(Long photoquestId) {
        this.photoquestId = photoquestId;
    }
}
