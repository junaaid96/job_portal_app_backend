package org.example.jobportalappbackend.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Times every {@code JobService} method. {@code @Around} is the only advice that can run code both before and
 * after the call, and the only one that sees the return value. {@code proceed()} continues the chain
 * (the logging aspect, then the real method).
 *
 * <p>{@code @Order(1)} is the outer aspect: its timer includes the logging advice. Lower order value means
 * higher precedence in Spring.
 *
 * <p>The previous version of this project also had a validation aspect that rewrote a negative id into a
 * positive one. That hid bad input. Id checks now live on the controller ({@code @Positive}) and in the service.
 */
@Component
@Aspect
@Order(1)
public class PerformanceMonitorAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitorAspect.class);

    @Around("execution(* org.example.jobportalappbackend.service.JobService.*(..))")
    public Object monitorTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
            log.info("{} took {} ms", joinPoint.getSignature().toShortString(), elapsedMs);
        }
    }
}
