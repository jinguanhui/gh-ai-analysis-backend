package com.jgh.aianalysis.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.OrderService;
import com.jgh.aianalysis.mapper.OrderMapper;
import org.springframework.stereotype.Service;

/**
 * @author 15180
 * @description 针对表【order(图表信息表)】的数据库操作Service实现
 * @createDate 2026-02-05 11:08:03
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order>
        implements OrderService {

}




