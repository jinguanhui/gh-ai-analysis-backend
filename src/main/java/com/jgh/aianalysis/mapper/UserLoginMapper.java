package com.jgh.aianalysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jgh.ghcommon.model.entity.UserLogin;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 15180
* @description 针对表【user_login(用户登录信息表)】的数据库操作Mapper
* @createDate 2026-01-07 17:51:14
* @Entity generator.domain.UserLogin
*/
@Mapper
public interface UserLoginMapper extends BaseMapper<UserLogin> {

    /**
     * 获取用户最新的登录信息
     * @param userId
     * @return
     */
    UserLogin getMostNewUserLoginByUserId(Long userId);
}




