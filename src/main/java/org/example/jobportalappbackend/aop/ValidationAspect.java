package org.example.jobportalappbackend.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ValidationAspect {
    private static final Logger logger = LoggerFactory.getLogger(ValidationAspect.class);

    @Around("execution(* org.example.jobportalappbackend.service.JobService.getJobPost(..)) && args(postId)")
    public Object validateAndUpdate(ProceedingJoinPoint joinPoint, int postId) throws Throwable {
        if(postId < 0) {
            logger.error("Post Id cannot be negative!");
            postId = -postId;
            logger.info("Post Id converted to positive: " + postId);
        }
        Object jpObj = joinPoint.proceed(new Object[]{postId});

        return jpObj;
    }
}
