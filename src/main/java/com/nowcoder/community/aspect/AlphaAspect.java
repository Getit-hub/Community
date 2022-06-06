package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/*@Component
@Aspect*/
public class AlphaAspect {

    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){
    }

    @Before("pointcut()")
    public void before(){
        System.out.println("before...");
    }

    @After("pointcut()")
    public void after(){
        System.out.println("after...");
    }

    @After("pointcut()")
    public void afterReturning(){
        System.out.println("afterReturn...");
    }

    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        System.out.println("afterThrowing...");
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("aroundbefore...");
        Object obj = joinPoint.proceed();
        System.out.println("aroundafter...");
        return obj;
    }


}
