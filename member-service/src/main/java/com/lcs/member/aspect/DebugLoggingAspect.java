package com.lcs.member.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
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

    @Pointcut("execution(* com.lcs.member.presentation..*.*(..))")
    public void controllerLayer() {
    }

    @Pointcut("execution(* com.lcs.member.application.service..*.*(..))")
    public void serviceLayer() {
    }

    @Before("controllerLayer() || serviceLayer()")
    public void before(JoinPoint joinPoint) {
        LOGGER.atInfo().log(
                "{}.{}() invoked with arguments: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }

    @AfterReturning("controllerLayer() || serviceLayer()")
    public void afterReturning(JoinPoint joinPoint) {
        LOGGER.atInfo().log(
                "{}.{}() executed with arguments: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                joinPoint.getArgs());
    }

    @AfterThrowing(pointcut = "controllerLayer() || serviceLayer()", throwing = "exception")
    public void afterThrowing(JoinPoint joinPoint, Throwable exception) {
        LOGGER.atError().log(
                "{}.{}() threw {}: {}",
                joinPoint.getSignature().getDeclaringType().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }
}
