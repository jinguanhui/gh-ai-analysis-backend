package com.jgh.aianalysis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.service.UserLoginService;
import com.jgh.aianalysis.mapper.UserLoginMapper;
import com.jgh.ghcommon.model.entity.UserLogin;
import org.springframework.stereotype.Service;

/**
* @author 15180
* @description 针对表【user_login(用户登录信息表)】的数据库操作Service实现
* @createDate 2026-01-07 17:51:14
*/
@Service
public class UserLoginServiceImpl extends ServiceImpl<UserLoginMapper, UserLogin>
    implements UserLoginService{

}




