package com.tiksem.pq.db;

/**
 * Created by CM on 11/14/2014.
 */
public class LimitOffset {
    private long limit = 10;
    private long offset = 0;

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
