package com.jgh.aianalysis.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.IPUtils;
import com.jgh.ghcommon.model.entity.AccessKey;
import com.jgh.ghcommon.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Component
public class MyInterceptor implements HandlerInterceptor {

    private final AccessKeyService accessKeyService;
    private final UserService userService;

    public MyInterceptor(AccessKeyService accessKeyService, UserService userService) {
        this.accessKeyService = accessKeyService;
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String auth = request.getHeader("auth");
        String requestURL = request.getRequestURL().toString();
        String requestMethod = request.getMethod();
        String header = request.getHeaderNames().toString();
        String localHost = request.getLocalAddr();
        String cookie = Arrays.toString(request.getCookies());
        String url = request.getServletPath();
        log.info(request.getHeader("userId"));
        log.info("用户的请求URL:{}", requestURL);
        log.info("用户的请求方法:{}", requestMethod);
        log.info("用户的请求IP:{}", IPUtils.getIpAddr(request));
        log.info("用户的请求头:{}", header);
        log.info("用户的Cookie:{}", cookie);
        log.info("用户的请求地址:{}", localHost);

        if (auth == null || !auth.equals("ghai")) {
            log.error("该请求不是从网关中转发的，拦截违法路径");
            return false;
        }

        if (url.contains("/chart/gen")) {
            Long userId = Long.valueOf(request.getHeader("userId"));
            String encryptPublicKey = request.getHeader("signature");

            QueryWrapper<AccessKey> wrapper = new QueryWrapper<>();
            wrapper.eq("userId", userId);
            AccessKey accessKey = accessKeyService.getOne(wrapper);
            String privateKey = accessKey.getPrivateKey();
            String aesKey = accessKey.getAesKey();

            String salt = null;


        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);

    }
}
