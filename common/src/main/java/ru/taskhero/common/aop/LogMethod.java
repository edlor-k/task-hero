package ru.taskhero.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для логирования"
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethod {

    /**
     * Описание операции для логов, например "menu-create" или "dish-update"
     */
    String value() default "";

    /**
     * Нужно ли логировать входные параметры
     */
    boolean logArgs() default true;

    /**
     * Нужно ли логировать возвращаемый результат
     */
    boolean logResult() default true;
}
