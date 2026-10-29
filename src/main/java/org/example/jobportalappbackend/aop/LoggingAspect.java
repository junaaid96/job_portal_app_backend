package org.example.jobportalappbackend.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting log lines for {@link org.example.jobportalappbackend.service.JobService}.
 *
 * <p>{@code @Aspect} marks the class as advice. {@code @Component} puts it in the context so Boot's AspectJ
 * auto-proxy can wrap the service. You do not call these methods yourself.
 *
 * <p>The string {@code execution(* org.example...JobService.*(..))} is a pointcut:
 * any return type, any method on JobService, any arguments. It matches public methods called from another bean.
 * A private call inside JobService does not pass the proxy, so it is not logged. That is a Spring AOP limit, not a bug.
 *
 * <p>{@code @Order(2)} runs this aspect inside the timing aspect ({@code @Order(1)} is the outer one).
 */
@Component
@Aspect
@Order(2)
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /** Runs before the service method. {@link JoinPoint} is the call that is about to happen. */
    @Before("execution(* org.example.jobportalappbackend.service.JobService.*(..))")
    public void logMethodCall(JoinPoint joinPoint) {
        log.info("Calling {}", joinPoint.getSignature().toShortString());
    }

    /**
     * {@code @After} runs after the method whether it returned or threw (the old name was "after finally").
     * {@code @AfterReturning} would skip failures. This one is narrowed to the list method so the log shows
     * both a broad pointcut and a narrow one.
     */
    @After("execution(* org.example.jobportalappbackend.service.JobService.getJobPosts(..))")
    public void logListCompleted(JoinPoint joinPoint) {
        log.info("Finished {}", joinPoint.getSignature().toShortString());
    }
}
