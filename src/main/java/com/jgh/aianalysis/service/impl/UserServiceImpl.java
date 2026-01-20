package com.jgh.aianalysis.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.mapper.UserLoginMapper;
import com.jgh.aianalysis.mapper.UserMapper;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.IPUtils;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.ResponseCode;
import com.jgh.ghcommon.common.UserLoginEnum;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author 15180
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-12-23 18:52:08
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {


    @Resource
    private RedisUtil redisUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserLoginMapper userLoginMapper;

    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            log.info("用户注册失败，参数为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        if (userAccount.length() < 4) {
            log.info("用户注册失败，账号长度不小于4");
            throw new BusinessException(ResponseCode.USER_LOGIN_ACCOUNT_LENGTH_ERROR);

        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            log.info("用户注册失败，密码长度不小于8");
            throw new BusinessException(ResponseCode.USER_LOGIN_PASSWORD_LENGTH_ERROR);
        }

        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ResponseCode.USER_LOGIN_ACCOUNT_INVALID_ERROR);
        }

        // 账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ResponseCode.USER_ACCOUNT_EXIST);
        }
        // 2.加密
        String salt = RandomUtil.randomNumbers(5);
        String encryptPassword = DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
        // 3.插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setSalt(salt);
        String randomNumbers = RandomUtil.randomNumbers(4);
        String username = "用户GH" + randomNumbers;
        user.setUsername(username);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException("用户数据数据库插入失败！");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request, HttpServletResponse response) {
        // 1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            log.info("用户登录失败，参数为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        if (userAccount.length() < 4) {
            log.info("用户登录失败，账号长度不小于4");
            throw new BusinessException(ResponseCode.USER_LOGIN_ACCOUNT_LENGTH_ERROR);
        }
        if (userPassword.length() < 8) {
            log.info("用户登录失败，密码长度不小于8");
            throw new BusinessException(ResponseCode.USER_LOGIN_PASSWORD_LENGTH_ERROR);
        }

        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ResponseCode.USER_LOGIN_ACCOUNT_INVALID_ERROR);

        }

        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("用户登录失败，账号错误");
            throw new BusinessException("用户登录失败，账号错误");
        }

        if (user.getUserStatus() == 1) {
            log.info("用户登录失败，用户被封禁");
            throw new BusinessException("用户被封禁");
        }
        String salt = user.getSalt();
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
        if (!encryptPassword.equals(user.getUserPassword())) {
            log.info("用户登录失败，密码错误");
            throw new BusinessException("密码错误！");
        }

        //  生成jwt令牌设置到返回的用户数据中

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("expireTime", DateUtil.offsetDay(new Date(), 7));
        payload.put("userRole", user.getUserRole());
        payload.put("nonce", RandomUtil.randomNumbers(5));

        String refreshToken = JWTUtil.createToken(payload, user.getUserPassword().getBytes());
        log.info("refreshToken: " + refreshToken);

        //  将refreshToken设置到redis中，以便后续的异地登录检测--和refreshToken一样设置7天过期
        String uerId = user.getId().toString();
        //  用userId作为key，refreshToken作为value
        redisUtil.set(uerId + ":refreshToken", refreshToken, 7 * 24 * 60 * 60);

        String s = redisUtil.get(uerId + ":refreshToken");
        log.info("redis: " + s);

        HashMap<String, Object> payload2 = new HashMap<>();
        payload2.put("id", user.getId());
        payload2.put("expireTime", DateUtil.offsetHour(new Date(), 1));
        payload2.put("userRole", user.getUserRole());
        String token = JWTUtil.createToken(payload2, user.getUserPassword().getBytes());
        user.setToken(token);
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
        userLogin.setUserId(user.getId());
        userLogin.setLoginPath(pathInfo);
        userLogin.setDescription(UserLoginEnum.VALID_LOGIN.getDesc());
        userLogin.setLoginStatus(UserLoginEnum.VALID_LOGIN.getStatus().longValue());

        int insert = userLoginMapper.insert(userLogin);
        if (insert < 1) {
            log.info("用户登录失败，登录信息数据库插入失败！");
            throw new BusinessException("用户登录信息数据库插入失败！");
        }

        // 3.用户脱敏
        return getSafetyUser(user);
    }

    @Override
    public User getSafetyUser(User user) {
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setUserRole(user.getUserRole());
        safetyUser.setUserStatus(user.getUserStatus());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setToken(user.getToken());
        safetyUser.setInvokeCount(user.getInvokeCount());
        return safetyUser;
    }

}




