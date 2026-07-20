package com.lcs.course.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DebugLoggingAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugLoggingAspect.class);

    @Pointcut("execution(* com.lcs.course.presentation..*.*(..))")
    public void controllerLayer() {
    }

    @Pointcut("execution(* com.lcs.course.application.service..*.*(..))")
    public void serviceLayer() {
    }

    @Before("controllerLayer() || serviceLayer()")
    public void before(JoinPoint joinPoint) {
        LOGGER.atInfo().log(
                "{}() invoked with arguments: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }

    @After("controllerLayer() || serviceLayer()")
    public void after(JoinPoint joinPoint) {
        LOGGER.atInfo().log(
                "{}() executed with arguments: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }
}
