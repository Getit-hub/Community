package com.nowcoder.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceLogAspect {

    /**
     * 访问服务日志，记录用户访问的服务
     */
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);


    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){

    }

    /**
     * 记录用户[1,2,3,4] 在[xxx],访问了[com.nowcoder.community.service.xxx())].
     * @param joinPoint
     */
    @Before("pointcut()")
    public void before(JoinPoint joinPoint){//JoinPoint对象封装了切点方法
        //在Web开发中，service层或者某个工具类中需要获取到HttpServletRequest对象还是比较常见的。
        // 一种方式是将HttpServletRequest作为方法的参数从controller层一直放下传递，不过这种有点费劲，且做起来不是优雅；
        // 还有另一种则是RequestContextHolder，直接在需要用的地方使用如下方式取HttpServletRequest即可，使用代码如下：
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        //实际上，HttpServletRequest和HttpServletRespond都是被封装在RequestAttributes对象里面的
        //所以可以通过RequestAttributes对象来直接获取HttpServletRequest对象
        //根据请求参数获取请求
        if (attributes==null){
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        //根据请求对象获取请求地址
        String ip = request.getRemoteHost();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Signature signature = joinPoint.getSignature();//这个方法返回的是切入点的签名对象，它里面含有的是切入点的一些信息
        String target = signature.getDeclaringTypeName() + "." + signature.getName();//获取切点方法所属的类名和方法名
        logger.info(String.format("用户[%s],在[%s],访问了[%s]",ip,now,target));
    }

}
