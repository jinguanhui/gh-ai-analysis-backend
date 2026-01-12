package com.jgh.aianalysis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.mapper.AccessKeyMapper;
import com.jgh.ghcommon.model.entity.AccessKey;
import org.springframework.stereotype.Service;

/**
 * @author 15180
 * @description 针对表【access_key(用户密钥表)】的数据库操作Service实现
 * @createDate 2026-01-09 16:42:24
 */
@Service
public class AccessKeyServiceImpl extends ServiceImpl<AccessKeyMapper, AccessKey>
        implements AccessKeyService {

}




