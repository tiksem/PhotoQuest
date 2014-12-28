package com.tiksem.mysqljava.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by CM on 12/27/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Index {
    IndexType indexType() default IndexType.BTREE;
    String type() default "";
}
