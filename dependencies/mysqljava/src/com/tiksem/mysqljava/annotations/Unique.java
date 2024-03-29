package com.tiksem.mysqljava.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by CM on 12/27/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {
    String type() default "";
    IndexType indexType() default IndexType.BTREE;
}
