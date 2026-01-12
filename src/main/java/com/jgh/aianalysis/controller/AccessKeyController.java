package com.jgh.aianalysis.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.AccessKeyEnum;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.entity.AccessKey;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.vo.AccessKeyVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/accessKey")
@Slf4j
public class AccessKeyController {

    private static final String REDIS_KEY_PUBLIC_KEY_ENCRYPT_AES = ":publicKeyEncryptAes";
    @Resource
    private AccessKeyService accessKeyService;

    @Resource
    private UserService userService;

    @Resource
    private RedisUtil redisUtil;

    @PostMapping("/list")
    public BaseResponse<List<AccessKeyVo>> listAccessKeys(HttpServletRequest request) {
        log.info("listAccessKeys开始");
        String userId = request.getHeader("userId");
        QueryWrapper<AccessKey> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", Long.valueOf(userId));
        wrapper.eq("status", AccessKeyEnum.Able.getStatus());
        wrapper.ge("expireTime", new Date());

        List<AccessKey> accessKey = accessKeyService.list(wrapper);
        if (accessKey == null || accessKey.isEmpty()) {
            throw new BusinessException("列表展示密钥对失败！！！可能列表为空");
        }
        AccessKey accessKey1 = accessKey.getFirst();
        AccessKeyVo accessKeyVo = new AccessKeyVo();
        BeanUtils.copyProperties(accessKey1, accessKeyVo);

        //  从redis中获取publicKeyEncryptAes
        String publicKeyEncryptAes = null;
        try {
            publicKeyEncryptAes = redisUtil.get(userId + REDIS_KEY_PUBLIC_KEY_ENCRYPT_AES);
        } catch (Exception e) {
            throw new BusinessException("redis获取失败！！！");
        }

        Duration betweenDuration = Duration.between(LocalDateTimeUtil.now(),
                LocalDateTimeUtil.of(accessKey1.getExpireTime()));
        //  获取当前时间与过期时间之间的日期间隔
        long days = betweenDuration.toDays();
        long hours = betweenDuration.toHours() % 24;
        accessKeyVo.setLeftTime(days);
        accessKeyVo.setLeftTimeHour(hours);
        accessKeyVo.setEncryptPublicKey(publicKeyEncryptAes);

        return BaseResponse.success(List.of(accessKeyVo));
    }

    @PostMapping("/create")
    public BaseResponse<AccessKeyVo> createAccessKeys(HttpServletRequest request) {
        log.info("createAccessKeys开始");
        String userId = request.getHeader("userId");
        User currentUser = userService.getById(userId);
        QueryWrapper<AccessKey> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", Long.valueOf(userId));
        wrapper.eq("status", AccessKeyEnum.Able.getStatus());
        List<AccessKey> list = accessKeyService.list(wrapper);
        if (list != null && list.size() > 0) {
            log.info("用户已创建过密钥对");
            throw new BusinessException("最多只能创建1对可访问密钥对！请先禁用旧的密钥对，防止密钥对泄露");
        }


        String userAccount = currentUser.getUserAccount();
        // 2. 加密
        //  分配aesKey
        //  用aesKey作为AES的秘钥去对一段数据加密
        //  再用rsa工具类生成公钥和私钥



        //  用公钥对aesKey进行加密返回给前端展示


        //  将公钥加密后的AES密钥保存到redis中

        AccessKey accessKeyUser = new AccessKey();
        accessKeyUser.setUserId(Long.valueOf(userId));
        accessKeyUser.setStatus(AccessKeyEnum.Able.getStatus());

        boolean save = accessKeyService.save(accessKeyUser);

        if (!save) {
            throw new BusinessException("创建访问密钥失败！");
        }


        return BaseResponse.success(null);
    }

    @PostMapping("/disable")
    public BaseResponse<Boolean> disableAccessKeys(HttpServletRequest request) {
        log.info("disableAccessKeys开始");
        String userId = request.getHeader("userId");

        UpdateWrapper<AccessKey> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("userId", Long.valueOf(userId));
        updateWrapper.set("status", AccessKeyEnum.Disable.getStatus());
        updateWrapper.set("isDelete", 1);

        boolean save = accessKeyService.update(updateWrapper);

        if (!save) {
            throw new BusinessException("禁用访问密钥失败！");
        }

        //  删除publicKeyEncryptAes缓存
        redisUtil.remove(userId+REDIS_KEY_PUBLIC_KEY_ENCRYPT_AES);


        return BaseResponse.success(save);
    }

    @PostMapping("/idletime")
    public BaseResponse<Boolean> idleTimeAccessKeys(@RequestParam("idleTime") Integer idleTime, HttpServletRequest request) {
        log.info("idleTimeAccessKeys开始");
        String userId = request.getHeader("userId");
        QueryWrapper<AccessKey> wrapper = new QueryWrapper<>();
        wrapper.eq("userId", Long.valueOf(userId));
        AccessKey accessKey = accessKeyService.getOne(wrapper);

        UpdateWrapper<AccessKey> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("userId", Long.valueOf(userId));
        updateWrapper.set("expireTime", DateUtil.offsetDay(accessKey.getCreateTime(),idleTime));

        boolean save = accessKeyService.update(updateWrapper);

        if (!save) {
            throw new BusinessException("禁用访问密钥失败！");
        }
        return BaseResponse.success(save);
    }

}
