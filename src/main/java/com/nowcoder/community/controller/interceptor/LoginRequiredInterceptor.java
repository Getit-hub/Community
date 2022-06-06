package com.nowcoder.community.controller.interceptor;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.util.HostHolders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolders hostHolders;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){
            //因为请求有可能是静态资源请求，也有可能是动态的处理器方法请求
            //我们只对动态方法请求进行拦截
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //获取处理请求的处理器所使用的对应方法
            Method method = handlerMethod.getMethod();
            //获取我们的自定义注解
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            //如果该方法被我们的自定义注解标识而且本地线程变量中没有用户（说明此线程没有用户处于登录状态）
            //进行拦截 将路径重定向到登录页面
            if(loginRequired!=null && hostHolders.getUser()==null){
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
