package com.tiksem.pq.data;

import com.tiksem.pq.exceptions.ExceptionWithData;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Created by CM on 11/7/2014.
 */
public class ExceptionResponse {
    public String error;
    public String errorClass;
    public String message;
    public String cause;
    public Object data;

    public ExceptionResponse(Throwable throwable) {
        Class<? extends Throwable> aClass = throwable.getClass();
        errorClass = aClass.getCanonicalName();
        error = aClass.getSimpleName();
        message = throwable.getMessage();

        Throwable throwableCause = throwable.getCause();
        if(throwableCause != null){
            cause = throwableCause.getClass().getCanonicalName() + " "
                    + throwable.getMessage();
        }

        if(throwable instanceof ExceptionWithData){
            data = ((ExceptionWithData) throwable).getData();
        } else if(throwable instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException exception = (MissingServletRequestParameterException)throwable;
            data = exception.getParameterName();
        }
    }

    public ExceptionResponse() {
    }
}
