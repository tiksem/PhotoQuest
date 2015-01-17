package com.tiksem.pq.exceptions;

/**
 * Created by CM on 1/18/2015.
 */
public class ImageSizeException extends ExceptionWithData {
    public ImageSizeException() {
    }

    public ImageSizeException(String message) {
        super(message);
    }

    public ImageSizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageSizeException(Throwable cause) {
        super(cause);
    }

    public ImageSizeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
