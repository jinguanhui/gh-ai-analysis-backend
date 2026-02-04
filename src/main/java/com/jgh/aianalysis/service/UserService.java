package com.jgh.aianalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jgh.ghcommon.model.dto.user.UserQueryVo;
import com.jgh.ghcommon.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author jgh
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2025-12-23 18:52:08
 */
public interface UserService extends IService<User> {


    /**
     * 用户注释
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      请求信息
     * @param response
     * @return
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取脱敏后的用户数据
     *
     * @param user 元数据（为脱敏）
     * @return 脱敏后的用户数据
     */
    User getSafetyUser(User user);


}
