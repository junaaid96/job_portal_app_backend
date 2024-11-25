package org.example.jobportalappbackend.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // return type, class_name.method_name(parameters)
    // this is a pointcut expression in AspectJ. It specifies the signature of the methods that the aspect should intercept.

    @Before("execution(* org.example.jobportalappbackend.service.JobService.*(..))")
    public void logMethodCall(JoinPoint joinPoint) {
        logger.info("Method called: " + joinPoint.getSignature().getName());
    }

    // After (by default After is AfterFinally), AfterReturning, AfterThrowing, Around
    @After("execution(* org.example.jobportalappbackend.service.JobService.getJobPosts(..))")
    public void logMethodExecuted(JoinPoint joinPoint) {
        logger.info("Method executed: " + joinPoint.getSignature().getName());
    }
}

// JoinPoint is an object that represents the method being executed. It provides access to the method signature, arguments, and other information.
