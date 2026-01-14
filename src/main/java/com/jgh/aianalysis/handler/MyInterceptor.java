package com.jgh.aianalysis.handler;

import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.utils.IPUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class MyInterceptor implements HandlerInterceptor {

    @Resource
    private AccessKeyService accessKeyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String auth = request.getHeader("auth");
        String requestURL = request.getRequestURL().toString();
        String requestMethod = request.getMethod();
        String header = request.getHeaderNames().toString();
        String localHost = request.getLocalAddr();
        String cookie = Arrays.toString(request.getCookies());
        String url = request.getServletPath();
        log.info("用户的请求URL:{}", requestURL);
        log.info("用户的请求方法:{}", requestMethod);
        log.info("用户的请求IP:{}", IPUtils.getIpAddr(request));
        log.info("用户的请求头:{}", header);
        log.info("用户的Cookie:{}", cookie);
        log.info("用户的请求地址:{}", localHost);
        log.info("用户的请求ServletPath:{}", url);

        if (auth == null || !auth.equals("ghai")) {
            log.error("该请求不是从网关中转发的，拦截违法路径");
            return false;
        }

        if (request.getServletPath().equals("/chart/gen")) {
            // 检查是否为multipart请求
            if (request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/")) {
                // 对于multipart请求，我们需要自定义包装器来处理解密
                CustomMultipartHttpServletRequest customMultipartHttpServletRequest
                        = new CustomMultipartHttpServletRequest(request, accessKeyService);
                // 通过RequestContextHolder设置包装后的请求
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                        new org.springframework.web.context.request.ServletRequestAttributes(customMultipartHttpServletRequest, response), true);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
