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
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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

        ServerHttpResponse response = exchange.getResponse();
        String token = request.getHeaders().getFirst("token");


        //  登录和注册接口不进行token校验
        if (request.getPath().value().equals("/api/user/login") ||
                request.getPath().value().equals("/api/user/register") || request.getPath().value().equals("/api/user/logout")) {
            return chain.filter(exchange.mutate().build());
        }

        //  校验refreshToken
        if (request.getPath().value().equals("/api/user/refreshToken")) {
            String refreshToken = Objects.requireNonNull(request.getCookies().getFirst("refreshToken")).getValue();
            log.info("refreshToken: {}", refreshToken);

            JWT jwt = JWTUtil.parseToken(refreshToken);
            Long id = Convert.toLong(jwt.getPayload("id"));
            DateTime expireTime = DateTime.of(Convert.toDate(jwt.getPayload("expireTime")));
            UserLogin userLoginInfo = new UserLogin();
            //  校验refreshToken是否为空
            if (refreshToken == null) {
                log.info("refreshToken为空,无权限");
                userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
                userLoginInfo.setUserId(id);
                userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
                userLoginInfo.setLoginPath(realIpAddress);

                innerUserService.insertLoginInfo(userLoginInfo);
                response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
                return response.setComplete();
            }

            //  校验refreshToken是否为空字符串
            if (StrUtil.isBlank(refreshToken)) {
                log.info("refreshToken为空字符串,无权限");
                userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
                userLoginInfo.setUserId(id);
                userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
                userLoginInfo.setLoginPath(realIpAddress);

                innerUserService.insertLoginInfo(userLoginInfo);
                response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
                return response.setComplete();
            }

            // 从jwt中获取用户信息（id）和过期时间


            long currentDateTime = new DateTime().getTime();

            // 尝试从redis中获取refreshToken
            String userRefreshToken = redisUtil.get(id.toString());

            if (!userRefreshToken.equals(refreshToken)) {
                log.info("refreshToken错误，请重新登录");
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
                log.info("用户登录信息不存在，请先登录");
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
                log.info("stamp为空,无法权限");
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
            if (requestDate > newDateTime && !realIpAddress.equals(loginPath)) {
                log.info("检查到用户异地登录，请重新登录");
// 将登录信息计入到数据库中
                // 将登录信息计入到数据库中
                userLoginInfo.setDescription(UserLoginEnum.INVALID_LOGIN.getDesc());
                userLoginInfo.setUserId(id);
                userLoginInfo.setLoginStatus(UserLoginEnum.INVALID_LOGIN.getStatus().longValue());
                userLoginInfo.setLoginPath(realIpAddress);

                innerUserService.insertLoginInfo(userLoginInfo);
                redisUtil.remove("refreshToken", refreshToken);
                response.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
                return response.setComplete();
            }


            // 判断refreshToken是否过期
            if (currentDateTime >= expireTime.getTime()) {
                log.info("refreshToken过期，请先登录权限");
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
                log.info("refreshToken验证失败，无权限");
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

        //  单独校验sse的token，从请求参数获取token
        if (request.getPath().value().contains("/chart/progress/")) {
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            List<String> list = queryParams.get("token");
            token = list.getFirst();

        }

        //  校验token是否为空
        if (token == null) {
            log.info("token为空,无权限");
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
            log.info("jwt过期，无权限");
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