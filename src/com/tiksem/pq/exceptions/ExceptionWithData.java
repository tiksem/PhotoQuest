package com.tiksem.pq.exceptions;

/**
 * Created by CM on 1/13/2015.
 */
public class ExceptionWithData extends RuntimeException {
    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ExceptionWithData() {
    }

    public ExceptionWithData(String message) {
        super(message);
    }

    public ExceptionWithData(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionWithData(Throwable cause) {
        super(cause);
    }

    public ExceptionWithData(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}