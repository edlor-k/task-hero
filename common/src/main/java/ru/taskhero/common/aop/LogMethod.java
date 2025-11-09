package ru.taskhero.common.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для логирования.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethod {

    /**
     * Описание операции для логов, например "user-create" или "task-update".
     *
     * @return описание операции
     */
    String value() default "";

    /**
     * Нужно ли логировать входные параметры.
     *
     * @return true, если нужно логировать параметры
     */
    boolean logArgs() default true;

    /**
     * Нужно ли логировать возвращаемый результат.
     *
     * @return true, если нужно логировать результат
     */
    boolean logResult() default true;
}
