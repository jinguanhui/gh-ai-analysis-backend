package com.jgh.ghaigateway.filters;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.jgh.ghaigateway.utils.HttpUtils;
import com.jgh.ghaigateway.utils.RedisUtil;
import com.jgh.ghcommon.common.UserLoginEnum;
import com.jgh.ghcommon.dubbo.service.InnerUserService;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.entity.UserLogin;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

/**
 * 自定义全局过滤器
 */
@Component
@Slf4j
public class GlobalRequestFilter implements GlobalFilter, Ordered {

    @Resource
    private RedisUtil redisUtil;

    @DubboReference
    private InnerUserService innerUserService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("custom global filter");

        //1.请求日志
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getPath().value();
        String method = request.getMethod().toString();
        String realIpAddress = HttpUtils.getIpAddress(request);
        log.info("请求唯一标识: {}", request.getId());
        log.info("请求路径: {}", path);
        log.info("请求方法: {}", method);
        log.info("请求参数: {}", request.getQueryParams());
        log.info("请求来源地址: {}", request.getRemoteAddress());
        String source = request.getLocalAddress().getHostString();
        log.info("请求来源地址: {}", source);
        log.info("IP：{}", realIpAddress);

        //  单独校验sse的token，从请求参数获取token
        if (request.getPath().value().contains("/chart/progress/") ||
                request.getPath().value().contains("/ai/chat")) {
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            List<String> list = queryParams.get("token");
            if (list == null) {
                log.error("无token,无权限");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }
            String token = list.getFirst();

            //  校验token是否为空
            if (token == null) {
                log.error("token为空,无权限");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }

            //  校验token是否为空字符串
            if (StrUtil.isBlank(token)) {
                log.info("token为空字符串,无权限");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }

            // 从jwt中获取用户信息（id）和过期时间
            JWT jwt = JWTUtil.parseToken(token);
            Long id = Convert.toLong(jwt.getPayload("id"));
            DateTime expireTime = DateTime.of(Convert.toDate(jwt.getPayload("expireTime")));

            long currentDateTime = new DateTime().getTime();

            // 判断token是否过期
            if (currentDateTime >= expireTime.getTime()) {
                log.error("jwt过期，无权限");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            //  根据用户id获取出用户信息
            User user = innerUserService.getUserById(id);

            // 校验token
            boolean verify = JWTUtil.verify(token, user.getUserPassword().getBytes());
            if (!verify) {
                log.info("token验证失败，无权限");
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }

            return chain.filter(exchange);
        }

        String stamp = request.getHeaders().getFirst("stamp");
        long currentTime = new Date().getTime();

        long timestamp = 0;
        try {
            timestamp = new Date(Long.parseLong(stamp)).getTime();
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }

        // 防重放攻击--校验时间窗口
        if (timestamp > currentTime || currentTime - timestamp > 5 * 60 * 1000) {
            log.info("时间检验失败，存在重放攻击");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // 检查Nonce是否已使用
        String nonce = request.getHeaders().getFirst("nonce");
        String nonceKey = "nonce:" + nonce;
        Boolean isFirstUse = redisUtil.setnx(nonceKey, stamp.toString(), 3600);

        if (isFirstUse == null || !isFirstUse) {
            log.info("随机数检验失败，存在重放攻击");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }


        //  登录和注册接口不进行token校验
        if (request.getPath().value().equals("/api/user/login") ||
                request.getPath().value().equals("/api/user/register") ||
                request.getPath().value().equals("/api/user/logout") ||
                request.getPath().value().equals("/api/sms/send") ||
                request.getPath().value().equals("/api/sms/verify") ||
                request.getPath().value().equals("/api/sms/mail/send") ||
                request.getPath().value().equals("/api/sms/mail/verify")) {
            log.info("登录和注册接口不进行token校验");
            return chain.filter(exchange.mutate().build());
        }

        //  校验refreshToken
        if (request.getPath().value().equals("/api/user/refreshToken")) {
            return safeRefreshTokenCheck(exchange, chain, request, realIpAddress, response);
        }

        //  token校验
        return safeTokenCheck(exchange, chain, request, response);
    }

    private Mono<Void> safeRefreshTokenCheck(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request, String realIpAddress, ServerHttpResponse response) {
        String token = request.getHeaders().getFirst("token");

        JWT jwtRefresh = JWTUtil.parseToken(token);
        Long idRefresh = Convert.toLong(jwtRefresh.getPayload("id"));

        String refreshToken = redisUtil.get(idRefresh + ":refreshToken");


        if (refreshToken == null) {
            log.error("refreshToken为空,无权限");
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }
        log.info("refreshToken: {}", refreshToken);

        JWT jwt = JWTUtil.parseToken(refreshToken);
        Long id = Convert.toLong(jwt.getPayload("id"));
        DateTime expireTime = DateTime.of(Convert.toDate(jwt.getPayload("expireTime")));


        User userById = innerUserService.getUserById(id);
        if (userById.getUserStatus() == 1) {
            log.error("用户被禁用，请重新登录");
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        UserLogin userLoginInfo = new UserLogin();
        //  校验refreshToken是否为空
        if (refreshToken == null) {
            log.error("refreshToken为空,无权限");
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        //  校验refreshToken是否为空字符串
        if (StrUtil.isBlank(refreshToken)) {
            log.error("refreshToken为空字符串,无权限");
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        // 从jwt中获取用户信息（id）和过期时间


        long currentDateTime = new DateTime().getTime();

        // 尝试从redis中获取refreshToken
        String userRefreshToken = redisUtil.get(id.toString() + ":refreshToken");
        log.info("userRefreshToken: {}", userRefreshToken);
        log.info("refreshToken:{}", refreshToken);

        if (userRefreshToken == null) {
            log.error("refreshToken不存在，请重新登录");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        if (!userRefreshToken.equals(refreshToken)) {
            log.error("refreshToken错误，请重新登录");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        //  进行用户异地登录检查，若用户出现异地登录，将refreshToken移除redis，并返回错误
        UserLogin userLogin = innerUserService.getUserLoginById(id);
        if (userLogin == null) {
            log.error("用户登录信息不存在，请先登录");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }
        Date createTime = userLogin.getCreateTime();

        String stamp = request.getHeaders().getFirst("stamp");
        if (stamp == null) {
            log.error("stamp为空,无法权限");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }
        long requestDate = new Date(Long.parseLong(stamp)).getTime();

        //  用户上次的最新登录时间+1小时
        long newDateTime = DateUtil.offsetHour(createTime, 1).getTime();


        String loginPath = userLogin.getLoginPath();
        //  检查用户是否在一个小时之内异地登录，防止refreshToken盗用
        if (requestDate <= newDateTime && !realIpAddress.equals(loginPath)) {
            log.error("检查到用户异地登录，请重新登录");
// 将登录信息计入到数据库中
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            redisUtil.remove(id+":refreshToken", refreshToken);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }


        // 判断refreshToken是否过期
        if (currentDateTime >= expireTime.getTime()) {
            log.error("refreshToken过期，请先登录权限");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        //  根据用户id获取出用户信息
        User user = innerUserService.getUserById(id);

        // 校验refreshToken
        boolean verify = JWTUtil.verify(refreshToken, user.getUserPassword().getBytes());
        if (!verify) {
            log.error("refreshToken验证失败，无权限");
            // 将登录信息计入到数据库中
            userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
            userLoginInfo.setUserId(id);
            userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
            userLoginInfo.setLoginPath(realIpAddress);

            innerUserService.insertLoginInfo(userLoginInfo);
            response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
            return response.setComplete();
        }

        //  从refreshToken中获取到用户id后查询数据库后将用户角色作为请求体转递给后端因为后端需要校验角色
        ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate()
                .header("userRole", String.valueOf(user.getUserRole()))
                .header("userId", String.valueOf(user.getId()))
                .build();
        //  将构造的请求体重新复制给赋给exchange，不然不会生效
        return chain.filter(exchange.mutate().request(serverHttpRequest).build());
    }

    private Mono<Void> safeTokenCheck(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpRequest request, ServerHttpResponse response) {
        String token = request.getHeaders().getFirst("token");


        //  校验token是否为空
        if (token == null) {
            log.error("token为空,无权限");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        //  校验token是否为空字符串
        if (StrUtil.isBlank(token)) {
            log.error("token为空字符串,无权限");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        // 从jwt中获取用户信息（id）和过期时间
        JWT jwt = JWTUtil.parseToken(token);
        Long id = Convert.toLong(jwt.getPayload("id"));
        DateTime expireTime = DateTime.of(Convert.toDate(jwt.getPayload("expireTime")));

        User userById = innerUserService.getUserById(id);
        if (userById.getUserStatus() == 1) {
            log.error("用户被禁用，请重新登录");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        long currentDateTime = new DateTime().getTime();

        // 判断token是否过期
        if (currentDateTime >= expireTime.getTime()) {
            log.error("jwt过期，无权限");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //  根据用户id获取出用户信息
        User user = innerUserService.getUserById(id);

        // 校验token
        boolean verify = JWTUtil.verify(token, user.getUserPassword().getBytes());
        if (!verify) {
            log.error("token验证失败，无权限");
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        //  从token中获取到用户id后查询数据库后将用户角色作为请求体转递给后端因为后端需要校验角色
        ServerHttpRequest serverHttpRequest = exchange.getRequest().mutate()
                .header("userRole", String.valueOf(user.getUserRole()))
                .header("userId", String.valueOf(user.getId()))
                .build();
        //  将构造的请求体重新复制给赋给exchange，不然不会生效
        return chain.filter(exchange.mutate().request(serverHttpRequest).build());
    }
    public int getOrder() {
        return -20;
    }

}