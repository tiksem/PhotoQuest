package com.tiksem.mysqljava.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by CM on 12/27/2014.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ForeignKey {
    Class parent();
    String field();
    OnDelete onDelete() default OnDelete.CASCADE;
    OnUpdate onUpdate() default OnUpdate.CASCADE;
    IndexType indexType() default IndexType.HASH;
    boolean unique() default false;
}
