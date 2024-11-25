package org.example.jobportalappbackend.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class PerformanceMonitorAspect {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorAspect.class);

    @Around("execution(* org.example.jobportalappbackend.service.JobService.*(..))")
    public Object monitorTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        logger.info(joinPoint.getSignature().getName() + " Method execution started at: " + startTime);

        Object jpObj = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        logger.info(joinPoint.getSignature().getName() + " Method execution completed at: " + endTime);

        long timeTaken = endTime - startTime;
        logger.info(joinPoint.getSignature().getName() +" Method execution time: " + timeTaken + " ms");

        return jpObj;
    }
}
