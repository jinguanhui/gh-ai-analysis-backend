package com.jgh.aianalysis.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teautil.models.RuntimeOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.mapper.UserLoginMapper;
import com.jgh.aianalysis.service.SmsService;
import com.jgh.aianalysis.service.ThirdPartyUserService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.IPRealRegionUtil;
import com.jgh.aianalysis.utils.IPUtils;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.ThirdPartyTypeEnum;
import com.jgh.ghcommon.common.UserLoginEnum;
import com.jgh.ghcommon.model.dto.sms.SmsChangePsdCodeVerifyDTO;
import com.jgh.ghcommon.model.entity.ThirdPartyUser;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.entity.UserLogin;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;


@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Resource
    private com.aliyun.dypnsapi20170525.Client dypnsClient;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UserService userService;

    @Resource
    private UserLoginMapper userLoginMapper;

    @Resource
    private ThirdPartyUserService thirdPartyUserService;

    /**
     * 电话验证码
     *
     * @param phone
     * @param templateCode
     * @return
     */
    @Override
    public Boolean sendLoginCode(String phone, String templateCode) {
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName("速通互联验证码")
                .setTemplateCode(templateCode)
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}");

        SendSmsVerifyCodeResponse response = null;
        try {
            response = dypnsClient.sendSmsVerifyCodeWithOptions(request, new RuntimeOptions());
        } catch (Exception e) {
            log.error("发送验证码失败");
            throw new BusinessException("发送验证码失败");
        }

// 发送成功后设置 Redis 锁，限制频率
        try {
            redisUtil.set("lockKey", phone, 60);
        } catch (Exception e) {
            throw new BusinessException("发送验证码失败,redis存储失败");
        }

        return true;
    }

    /**
     * 登录验证码验证
     * @param phone
     * @param code
     * @param request
     * @param response
     * @return
     */
    @Override
    public User verifyCode(String phone, String code, HttpServletRequest request, HttpServletResponse response) {
        checkCode(phone, code);

        return getUserThirdLogin(ThirdPartyTypeEnum.PHONE, phone, request, response);

    }

    /**
     * 修改手机号验证码验证
     *
     * @param phone
     * @param code
     * @param request
     * @param response
     * @return
     */
    @Override
    public Boolean verifyCodeUpdatePhone(String phone, String code, HttpServletRequest request, HttpServletResponse response) {
        checkCode(phone, code);
        String userId = request.getHeader("userId");

        if (StringUtils.isBlank(userId)) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }

        User user = new User();
        user.setId(Long.valueOf(userId));
        user.setPhone(phone);

        boolean b = userService.updateById(user);
        if (!b) {
            log.error("更新手机号失败");
            throw new BusinessException("更新手机号失败");
        }
        return true;
    }

    @Override
    public Boolean verifyChangePsdCode(SmsChangePsdCodeVerifyDTO dto, HttpServletRequest request, HttpServletResponse response) {
        String phone = dto.getPhone();
        String code = dto.getCode();
        String password = dto.getPassword();


        checkCode(phone, code);

        String userId = request.getHeader("userId");

        if (StringUtils.isBlank(userId)) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }

        String salt = RandomUtil.randomNumbers(5);
        String encryptPassword = DigestUtils.md5DigestAsHex((salt + password).getBytes());

        User user = new User();
        user.setId(Long.valueOf(userId));
        user.setUserPassword(encryptPassword);
        user.setSalt(salt);

        boolean b = userService.updateById(user);
        if (!b) {
            log.error("更新手机号失败");
            throw new BusinessException("更新手机号失败");
        }
        return true;
    }

    private void checkCode(String phone, String code) {
        //  验证码
        CheckSmsVerifyCodeRequest checkSmsVerifyCodeRequest = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setVerifyCode(code);

        CheckSmsVerifyCodeResponse checkSmsVerifyCodeResponse = null;
        try {
            checkSmsVerifyCodeResponse = dypnsClient.checkSmsVerifyCodeWithOptions(checkSmsVerifyCodeRequest, new RuntimeOptions());
        } catch (Exception e) {
            throw new BusinessException("验证码验证失败");
        }

        // 判断是否验证成功
        boolean isCheckSuccess = checkSmsVerifyCodeResponse.getBody() != null && "OK".equals(checkSmsVerifyCodeResponse.getBody().getCode());
        if (!isCheckSuccess) {
            throw new BusinessException("验证码验证失败");
        }
    }

    @Override
    public User verify(String email, String code, HttpServletRequest request, HttpServletResponse response) {
        //  从redis中获取验证码验证
        String emailCode = null;
        try {
            emailCode = redisUtil.get(email);
        } catch (Exception e) {
            throw new BusinessException("邮箱登录失败！！！");
        }

        //  注册第三方用户并登陆
        return getUserThirdLogin(ThirdPartyTypeEnum.EMAIL, email, request, response);
    }

    private User getUserThirdLogin(ThirdPartyTypeEnum  type, String connection, HttpServletRequest request, HttpServletResponse response) {
        QueryWrapper<ThirdPartyUser> wrapper = new QueryWrapper<>();
        wrapper.eq("provider_id", connection);

        ThirdPartyUser thirdPartyUserServiceOne = thirdPartyUserService.getOne(wrapper);

        if (thirdPartyUserServiceOne != null) {
            //  如果第三方用户已存在，则直接返回用户信息
            User user = userService.getById(thirdPartyUserServiceOne.getUserId());
            if (user.getUserStatus() == 1) {
                log.info("用户登录失败，用户被封禁");
                throw new BusinessException("用户被封禁");
            }
            refreshTokenSet(request, response, user);

            return userService.getSafetyUser(user);
        }

        //  第三方用户不存在
        //  注册第三方登录账号
        //  注册本系统用户
        User user = new User();
        String randomNumbers = RandomUtil.randomNumbers(4);
        String username = "用户GH" + randomNumbers;
        user.setUsername(username);
        user.setUserAccount(connection);
        String salt = RandomUtil.randomNumbers(5);
        String encryptPassword = DigestUtils.md5DigestAsHex((salt + RandomUtil.randomNumbers(8)).getBytes());
        user.setUserPassword(encryptPassword);
        if (type.getType() == "phone") {

            user.setPhone(connection);
        }else if (type.getType() == "email"){
            user.setEmail(connection);
        }
        user.setSalt(salt);
        user.setInvokeCount(10);
        boolean save = userService.save(user);
        if (!save) {
            throw new BusinessException(type.getType() + "登录失败");
        }

        User newUser = userService.getById(user.getId());

        //  注册第三方登录账号

        ThirdPartyUser thirdPartyUser = new ThirdPartyUser();
        thirdPartyUser.setProvider_type(type.getType());
        thirdPartyUser.setProvider_id(connection);
        thirdPartyUser.setProvider_account(connection);
        thirdPartyUser.setUserId(newUser.getId());

        boolean save1 = thirdPartyUserService.save(thirdPartyUser);
        if (!save1) {
            throw new BusinessException(type.getType() + "登录失败");
        }

        refreshTokenSet(request, response, newUser);


        return userService.getSafetyUser(user);
    }

    private void refreshTokenSet(HttpServletRequest request, HttpServletResponse response, User newUser) {
        //  颁发token
        //  生成jwt令牌设置到返回的用户数据中

        //  生成jwt令牌设置到返回的用户数据中
        //  将refreshToken设置到redis中，以便后续的异地登录检测--和refreshToken一样设置7天过期
        String uerId = newUser.getId().toString();
        String s = redisUtil.get(uerId + ":refreshToken");
        if (StringUtils.isNotBlank(s)) {
            log.error("该账号已经登录过了！");
            throw new BusinessException("该账号已经登录过了！");
        }


        HashMap<String, Object> payload = new HashMap<>();
        payload.put("id", newUser.getId());
        payload.put("expireTime", DateUtil.offsetDay(new Date(), 7));
        payload.put("userRole", newUser.getUserRole());
        payload.put("nonce", RandomUtil.randomNumbers(5));

        String refreshToken = JWTUtil.createToken(payload, newUser.getUserPassword().getBytes());
        log.info("refreshToken: " + refreshToken);

        //  将refreshToken设置到redis中，以便后续的异地登录检测--和refreshToken一样设置7天过期
        //  用userId作为key，refreshToken作为value
        redisUtil.set(uerId + ":refreshToken", refreshToken, 7 * 24 * 60 * 60);

        HashMap<String, Object> payload2 = new HashMap<>();
        payload2.put("id", newUser.getId());
        payload2.put("expireTime", DateUtil.offsetHour(new Date(), 1));
        payload2.put("userRole", newUser.getUserRole());
        String token = JWTUtil.createToken(payload2, newUser.getUserPassword().getBytes());
        newUser.setToken(token);
        log.info("token: " + token);

        // 3. 将refreshToken设置到httponly cookie
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true); // 防止XSS攻击
        refreshTokenCookie.setSecure(true); // 仅通过HTTPS传输
        refreshTokenCookie.setPath("/"); // 可访问的路径
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7天有效期
        response.addCookie(refreshTokenCookie);

        //  记录用户的登录信息
        UserLogin userLogin = new UserLogin();
        String pathInfo = IPUtils.getIpAddr(request);
        userLogin.setUserId(newUser.getId());
        userLogin.setLoginPath(pathInfo);
        userLogin.setDescription(UserLoginEnum.VALID_LOGIN.getDesc());
        userLogin.setLoginStatus(UserLoginEnum.VALID_LOGIN.getStatus().longValue());
        userLogin.setRegion(IPRealRegionUtil.getRegion(pathInfo));

        int insert = userLoginMapper.insert(userLogin);
        if (insert < 1) {
            log.info("用户登录失败，登录信息数据库插入失败！");
            throw new BusinessException("用户登录信息数据库插入失败！");
        }
    }
}
