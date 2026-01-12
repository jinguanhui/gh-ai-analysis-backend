package com.jgh.aianalysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jgh.ghcommon.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}