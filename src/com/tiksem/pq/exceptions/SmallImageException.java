package com.tiksem.pq.exceptions;

import java.util.HashMap;

/**
 * Created by CM on 1/18/2015.
 */
public class SmallImageException extends ImageSizeException {
    public SmallImageException(final int minWidth, final int minHeight) {
        super("The image is too small");
        setData(new HashMap<String, Object>(){
            {
                put("minWidth", minWidth);
                put("minHeight", minHeight);
            }
        });
    }
}
