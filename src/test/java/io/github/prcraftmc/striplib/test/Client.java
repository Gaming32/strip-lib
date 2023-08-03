package io.github.prcraftmc.striplib.test;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE, ElementType.TYPE_USE})
public @interface Client {
    boolean stripLambdas() default true;
}
