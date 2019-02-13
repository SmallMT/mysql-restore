package szelink.mt.interceptor;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @author mt
 * 此拦截器仅用于便利开发人员开发,
 * 它将会在控制台打印每次请求的url,url所在的controller以及本次请求
 * 相应的页面信息
 */
public class InformationInterceptor implements HandlerInterceptor {


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Method method = ((HandlerMethod) handler).getMethod();
        System.out.println("当前请求地址: " + request.getRequestURL().toString());
        System.out.println("当前访问类: " + method.getDeclaringClass().getName());
        System.out.println("当前访问方法: " + method.getName());
        String viewName = modelAndView == null ? "" : "templates/" + modelAndView.getViewName() + ".html";
        System.out.println("响应的页面: " + viewName);
    }


}
