package ru.taskhero.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Аспект для централизованного логирования вызовов методов,
 * * помеченных аннотацией {@link LogMethod}.
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Перехватывает вызовы всех методов, аннотированных {@link LogMethod},
     * и автоматически логирует их выполнение.
     *
     * @return результат выполнения целевого метода
     * @throws Throwable пробрасывает исключение, если целевой метод его выбросил
     */

    @Around("@annotation(ru.taskhero.common.aop.LogMethod)")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        LogMethod ann = method.getAnnotation(LogMethod.class);

        String op = ann.value().isEmpty() ? method.getName() : ann.value();


        Class<?> targetClass = pjp.getTarget() != null ? pjp.getTarget().getClass() : method.getDeclaringClass();
        Logger targetLog = LoggerFactory.getLogger(targetClass);

        Object[] args = pjp.getArgs();
        long start = System.currentTimeMillis();

        if (ann.logArgs()) {
            targetLog.info("{}: start, args={}", op, safeArgs(args));
        } else {
            targetLog.info("{}: start", op);
        }

        try {
            Object result = pjp.proceed();
            long dur = System.currentTimeMillis() - start;

            if (ann.logResult() && result != null)
                targetLog.info("{}: done in {} ms, result={}", op, dur, safeResult(result));
            else {
                targetLog.info("{}: done in {} ms", op, dur);
            }
            return result;
        } catch (Exception e) {
            long dur = System.currentTimeMillis() - start;
            targetLog.error("{}: error after {} ms: {}", op, dur, e.getMessage(), e);
            throw e;
        }
    }


    private String safeArgs(Object[] args) {
        String s = java.util.Arrays.toString(args);
        return s.length() > 1000 ? s.substring(0, 1000) + "...(truncated)" : s;
    }

    private String safeResult(Object res) {
        String s = String.valueOf(res);
        return s.length() > 1000 ? s.substring(0, 1000) + "...(truncated)" : s;
    }
}
