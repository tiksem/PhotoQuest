package com.tiksem.pq.data;

/**
 * Created by CM on 11/7/2014.
 */
public class ExceptionResponse {
    public String error;
    public String message;
    public String cause;

    public ExceptionResponse(Throwable throwable) {
        error = throwable.getClass().getCanonicalName();
        message = throwable.getMessage();

        Throwable throwableCause = throwable.getCause();
        if(throwableCause != null){
            cause = throwableCause.getClass().getCanonicalName() + " "
                    + throwable.getMessage();
        }
    }

    public ExceptionResponse() {
    }
}
