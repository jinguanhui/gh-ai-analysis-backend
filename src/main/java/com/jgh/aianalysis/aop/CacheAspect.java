package com.jgh.aianalysis.aop;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jgh.aianalysis.annotation.Cache;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.chart.ChartQueryRequest;
import com.jgh.ghcommon.model.entity.Chart;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.aspectj.lang.annotation.Aspect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jgh.ghcommon.constant.CommonConstant.REDIS_CACHE_KEY_PREFIX;


/**
 * @Author: 彭_德华
 * @Date: 2021-10-26 15:27
 */
@Component
@Aspect
@Slf4j
public class CacheAspect {

    @Resource
    private RedisUtil redisUtil; // json数据

    /**
     * aop切点
     * 拦截被指定注解修饰的方法
     */
    @Pointcut("@annotation(com.jgh.aianalysis.annotation.Cache)")
    public void cache() {
    }

    /**
     * 缓存操作
     *
     * @param pjp
     * @return
     */
    @Around("cache()")
    public Object toCache(ProceedingJoinPoint pjp) {

        log.info("进入缓存切面");

        try {
            // 思路： 设置存储的格式，获取即可

            Signature signature = pjp.getSignature();
            // 类名
            String className = pjp.getTarget().getClass().getSimpleName();
            // 方法名
            String methodName = signature.getName();

            // 参数处理
            Object[] args = pjp.getArgs();
            String params = "";
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    if (!(args[i] instanceof HttpServletRequest)) {
                        params += JSON.toJSONString(args[i]);
                    }
                }
            }
            if (StringUtils.isNotEmpty(params)) {
                //加密 以防出现key过长以及字符转义获取不到的情况
                params = DigestUtils.md5Hex(params);
            }

            // 获取controller中对应的方法
            Method method = ((MethodSignature) signature).getMethod();
            Parameter parameter = method.getParameters()[1];

            HttpServletRequest request = parameter.getType().isAssignableFrom(HttpServletRequest.class) ? (HttpServletRequest) args[1] : null;

            String userId = request.getHeader("userId");
            // 获取Cache注解
            Cache annotation = method.getAnnotation(Cache.class);
            long expire = annotation.expire();
            String name = annotation.name();

            // 访问redis（先尝试获取，没有则访问数据库）
            String redisKey = REDIS_CACHE_KEY_PREFIX + userId;
            String redisValue = redisUtil.get(redisKey);
            if (StringUtils.isNotEmpty(redisValue)) {
                // 不为空返回数据
               List<Chart> result = JSON.parseArray(redisValue, Chart.class);
                ChartQueryRequest chartQueryRequest = (ChartQueryRequest) args[0];
                int pageSize = chartQueryRequest.getPageSize();
                int current = chartQueryRequest.getCurrent();
                log.info("数据从redis缓存中获取,key: {}", redisKey);
                Page<Chart> page = new Page<>(current, pageSize);
                page.setRecords(result.subList((current - 1) * pageSize, Math.min(current * pageSize, result.size())));
                page.setTotal(result.size());
                if (result.size() % pageSize == 0) {
                    page.setPages(result.size() / pageSize);
                } else {
                    page.setPages(result.size() / pageSize + 1);
                }
                return BaseResponse.success(page); // 跳出方法
            }
            Object proceed = pjp.proceed();
            BaseResponse<Page<Chart>> baseResponse = (BaseResponse<Page<Chart>>) proceed;
            List<Chart> records = baseResponse.getData().getRecords();
            redisUtil.set(redisKey, JSON.toJSONString(records), expire);
            log.info("数据存入redis缓存,key: {}", redisKey);
            return proceed;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return BaseResponse.error("redis缓存错误，系统错误");
    }

}
