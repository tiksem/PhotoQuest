package com.tiksem.mysqljava;

import javax.jdo.Query;
import java.util.Map;

/**
 * Created by CM on 11/14/2014.
 */
public class OffsetLimit {
    public static final long MAX_LIMIT = 1000;

    private long limit = 10;
    private long offset = 0;

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        if(limit < 0){
            throw new IllegalArgumentException("Negative limit");
        }

        if(limit > MAX_LIMIT){
            limit = MAX_LIMIT;
        }

        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        if(offset < 0){
            throw new IllegalArgumentException("Negative offset");
        }

        this.offset = offset;
    }

    public OffsetLimit() {
    }

    public OffsetLimit(long offset, long limit) {
        this.limit = limit;
        this.offset = offset;
    }

    public void addToMap(Map map) {
        map.put("offset", offset);
        map.put("limit", limit);
    }
}
