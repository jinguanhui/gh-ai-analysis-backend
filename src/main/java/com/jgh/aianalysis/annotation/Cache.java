package com.jgh.aianalysis.annotation;

import java.lang.annotation.*;

/**
 * @Author: 光吾
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {

    /**
     * 过期时间，默认60s
     * @return
     */
    long expire() default  12 * 3600 * 1000;

    /**
     * 缓存标识name
     * @return
     */
    String name() default "";

}
