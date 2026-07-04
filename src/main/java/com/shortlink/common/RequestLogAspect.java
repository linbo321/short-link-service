package com.shortlink.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestLogAspect {
    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    @Around("within(com.shortlink.controller..*)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("{} completed in {} ms", joinPoint.getSignature().toShortString(), elapsed);
        }
    }
}
