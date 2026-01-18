package com.jgh.aianalysis.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis工具类
 *
 * @author: jgh
 */
@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String,String> redisTemplate;


    /**
     * 根据key获取值
     *
     * @param key 键
     * @return 值
     */
    public String get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }
    public void set(String key, String value, long time) {
        redisTemplate.opsForValue().setIfAbsent(key, value, time, TimeUnit.SECONDS);
    }
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void remove(String key, String value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * 删除所有key
     *
     * @param key 键
     */
    public void remove(String key) {
        Set<String> keys = redisTemplate.keys(key);
        redisTemplate.delete(keys);
    }
}