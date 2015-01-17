package com.tiksem.pq.exceptions;

/**
 * Created by CM on 1/13/2015.
 */
public class AspectMaxFactorRatioException extends ImageSizeException {
    public AspectMaxFactorRatioException(AspectRatioData data) {
        super("Invalid aspect ratio!");
        setData(data);
    }
}
