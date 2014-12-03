package com.tiksem.pq.data.response;

/**
 * Created by CM on 12/3/2014.
 */
public class ReplyResponse {
    private int type;
    private Object value;

    public ReplyResponse(int type, Object value) {
        this.type = type;
        this.value = value;
    }

    public ReplyResponse() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
