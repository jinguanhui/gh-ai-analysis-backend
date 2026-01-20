package com.jgh.aianalysis.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.AliyunOSSUtil;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.ResponseCode;
import com.jgh.ghcommon.constant.UserConstant;
import com.jgh.ghcommon.model.dto.user.UserLoginRequest;
import com.jgh.ghcommon.model.dto.user.UserQueryDto;
import com.jgh.ghcommon.model.dto.user.UserRegisterRequest;
import com.jgh.ghcommon.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



/**
 * 用户接口
 *
 * @author jgh
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private AliyunOSSUtil aliyunOSSUtil;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求体
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        log.info("用户注册", userRegisterRequest);
        if (userRegisterRequest == null) {
            log.info("用户注册失败，用户数据为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            log.info("用户注册失败，用户数据为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        Long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return BaseResponse.success(userId);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求体
     * @param request          http请求
     * @return 用户对象
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request, HttpServletResponse response) {
        log.info("用户登录");
        if (userLoginRequest == null) {
            log.info("用户登录失败，用户数据为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            log.info("用户登录失败，用户数据为空");
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        return BaseResponse.success(userService.userLogin(userAccount, userPassword, request, response));
    }



    /**
     * 用户登出
     *
     * @param response http响应
     * @return void
     */
    @PostMapping("/logout")
    public BaseResponse userLogout(HttpServletRequest request, HttpServletResponse response) {
        log.info("用户登出！");

        String userId = request.getHeader("userId");

        // 创建Cookie对象，名称与原refreshToken一致
        Cookie cookie = new Cookie("refreshToken", "");

        // 设置Cookie属性
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // 仅通过HTTPS传输
        cookie.setPath("/"); // 与原Cookie的Path保持一致
        cookie.setMaxAge(0); // 设置Max-Age为0（立即过期）
        // 添加到响应头
        response.addCookie(cookie);

        redisUtil.remove(userId + ":refreshToken");


        return BaseResponse.success("登出成功");

    }


    /**
     *
     * 刷新token
     *
     * @param request http请求
     * @return 新的token
     */
    @PostMapping("/refreshToken")
    public BaseResponse<String> refreshToken(HttpServletRequest request) {
        log.info("refreshToken验证成功，进行token的重新发布");
        String userId = request.getHeader("userId");

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("id", userId);
        User user = null;
        try {
            user = userService.getOne(wrapper);
        } catch (Exception e) {
            throw new BusinessException("数据库查询失败！");
        }

        //  生成jwt令牌设置到返回的用户数据中

        String refreshToken = request.getCookies()[0].getValue();

        log.info("refreshToken: {}" , refreshToken);

        HashMap<String, Object> payload2 = new HashMap<>();
        payload2.put("id", user.getId());
        payload2.put("expireTime", DateUtil.offsetDay(new Date(), 1));
        payload2.put("userRole", user.getUserRole());
        payload2.put("refreshToken", refreshToken);
        String token = JWTUtil.createToken(payload2, user.getUserPassword().getBytes());
        return BaseResponse.success(token);
    }

    @PostMapping("/search")
    public BaseResponse<List<User>> searchUsers(@RequestBody UserQueryDto user, HttpServletRequest request) {
        log.info("列表展示用户");
        if (!isAdmin(request)) {
            log.info("用户权限不足");
            throw new BusinessException(ResponseCode.NOT_AUTH);
        }
        return BaseResponse.success(userService.list(getUserQueryWrapper(user)).stream().map(
                        userItem -> userService.getSafetyUser(userItem))
                .collect(Collectors.toList()));
    }

    //  查询用户本人数据
    @GetMapping("/getone")
    public BaseResponse<User> getUser(HttpServletRequest request) {
        log.info("查询用户");
        Long currentUserId = Long.valueOf(request.getHeader("userId"));
        if (currentUserId == null) {
            log.info("用户查询失败，用户不存在");
            throw new BusinessException(ResponseCode.USER_UNKNOWN_ERROR);
        }
        User user = userService.getById(currentUserId);

        if (user == null) {
            log.info("用户查询失败，用户不存在");
            throw new BusinessException(ResponseCode.USER_UNKNOWN_ERROR);
        }
        return BaseResponse.success(userService.getSafetyUser(user));
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUsers(@RequestBody Long id, HttpServletRequest request) {
        log.info("删除用户");
        Long currentUserId = Long.valueOf(request.getHeader("userId"));
        if (currentUserId.equals( id)) {
            log.info("用户删除失败，不能删除自己");
            throw new BusinessException("用户删除失败，不能删除自己");
        }
        if (!isAdmin(request)) {
            log.info("用户权限不足");
            throw new BusinessException(ResponseCode.NOT_AUTH);
        }
        if (id <= 0) {
            log.info("用户删除失败，用户id小于0,用户不存在");
            throw new BusinessException(ResponseCode.USER_UNKNOWN_ERROR);
        }
        return BaseResponse.success(userService.removeById(id));
    }

    /**
     * 更新用户
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUsers(@RequestParam("file") MultipartFile multipartFile,
                                             User user,
                                             HttpServletRequest request) {
        log.info("更新用户:{}", user);
        UpdateWrapper<User> wrapper = getUserUpdateWrapper(user);

        if (!multipartFile.isEmpty()) {
            //  将图片上传至OSS
            String fileURL = null;
            try {
                 fileURL = aliyunOSSUtil.getFileURL(multipartFile);
            } catch (IOException e) {
                log.error("图片上传失败！！！");
                e.printStackTrace();
                throw new BusinessException("图片上传失败！！！");
            }

            if (fileURL == null) {
                log.error("图片上传失败！！！");
                throw new BusinessException("图片上传失败！！！");
            }

            // 将返回的URL进行内容检查

            //  检查通过，将URL保存至数据库
            wrapper.set("avatarUrl", fileURL);
        }

        return BaseResponse.success(userService.update(wrapper));
    }

    private boolean isAdmin(HttpServletRequest request) {
        String userRole = request.getHeader("userRole");
        return userRole != null && Objects.equals(userRole, String.valueOf(UserConstant.ADMIN_ROLE));
    }

    //  更新用户wrapper
    @NotNull
    private static UpdateWrapper<User> getUserUpdateWrapper(User user) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        Long id = user.getId();
        String username = user.getUsername();
        String userAccount = user.getUserAccount();
        String avatarUrl = user.getAvatarUrl();
        Integer gender = user.getGender();
        String email = user.getEmail();
        Integer userStatus = user.getUserStatus();
        String phone = user.getPhone();
        Date createTime = user.getCreateTime();
        Integer userRole = user.getUserRole();
        Integer invokeCount = user.getInvokeCount();

        updateWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        updateWrapper.set(StringUtils.isNotEmpty(username), "username", username);
        updateWrapper.set(StringUtils.isNotEmpty(userAccount), "userAccount", userAccount);
        updateWrapper.set(StringUtils.isNotEmpty(avatarUrl), "avatarUrl", avatarUrl);
        updateWrapper.set(ObjectUtils.isNotEmpty(gender), "gender", gender);
        updateWrapper.set(StringUtils.isNotEmpty(email), "email", email);
        updateWrapper.set(ObjectUtils.isNotEmpty(userStatus), "userStatus", userStatus);
        updateWrapper.set(StringUtils.isNotEmpty(phone), "phone", phone);
        updateWrapper.set(ObjectUtils.isNotEmpty(userRole), "userRole", userRole);
        updateWrapper.set(ObjectUtils.isNotEmpty(createTime), "createTime", createTime);
        updateWrapper.set(ObjectUtils.isNotEmpty(invokeCount), "invokeCount", invokeCount);

        return updateWrapper;
    }

    //  查询用户wrapper
    @NotNull
    private static QueryWrapper<User> getUserQueryWrapper(UserQueryDto user) {

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        Long id = user.getId();
        String username = user.getUsername();
        String userAccount = user.getUserAccount();
        Integer gender = user.getGender();
        String email = user.getEmail();
        Integer userStatus = user.getUserStatus();
        String phone = user.getPhone();
        Integer userRole = user.getUserRole();
        Date beginTime = user.getBeginTime();
        Date endTime = user.getEndTime();


        wrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        wrapper.like(StringUtils.isNotEmpty(username), "username", username);
        wrapper.like(StringUtils.isNotEmpty(userAccount), "userAccount", userAccount);
        wrapper.eq(ObjectUtils.isNotEmpty(gender), "gender", gender);
        wrapper.like(StringUtils.isNotEmpty(email), "email", email);
        wrapper.eq(ObjectUtils.isNotEmpty(userStatus), "userStatus", userStatus);
        wrapper.like(StringUtils.isNotEmpty(phone), "phone", phone);
        wrapper.eq(ObjectUtils.isNotEmpty(userRole), "userRole", userRole);
        // 根据时间范围查询--beginTime, endTime
        wrapper.between(ObjectUtils.isNotEmpty(beginTime) && ObjectUtils.isNotEmpty(endTime), "createTime", beginTime, endTime);


        return wrapper;

    }


}