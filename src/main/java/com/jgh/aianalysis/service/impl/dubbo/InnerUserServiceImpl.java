package com.jgh.aianalysis.service.impl.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.mapper.UserLoginMapper;
import com.jgh.aianalysis.service.UserService;
import com.jgh.ghcommon.dubbo.service.InnerUserService;
import com.jgh.ghcommon.model.entity.User;
import com.jgh.ghcommon.model.entity.UserLogin;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;


/***
 * dubbo内部服务实现类--服务提供方
 * @author jgh
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Resource
    private UserLoginMapper userLoginMapper;

    /**
     * 根据用户id获取用户信息
     * @param id
     * @return
     */
    @Override
    public User getUserById(Long id) {
        QueryWrapper<User> Wrapper = new QueryWrapper<>();
        Wrapper.eq("id", id);

        User oriUser = userService.getOne(Wrapper);
        return oriUser;
    }

    /**
     * 根据用户id获取用户登录信息
     * @param userId
     * @return
     */
    @Override
    public UserLogin getUserLoginById(Long userId) {
        return userLoginMapper.getMostNewUserLoginByUserId(userId);
    }

    /**
     * 插入用户登录信息
     * @param userLogin
     * @return
     */
    public int insertLoginInfo(UserLogin userLogin) {
        int insert = userLoginMapper.insert(userLogin);
        if (insert < 1) {
            throw new BusinessException("用户登录信息数据库插入失败！");
        }
        return insert;
    }
}
