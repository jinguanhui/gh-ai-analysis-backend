package com.jgh.aianalysis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jgh.aianalysis.constant.PayStatusEnum;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.modal.dto.OrderDetailDto;
import com.jgh.aianalysis.modal.dto.OrderPageDto;
import com.jgh.aianalysis.modal.dto.OrderPayDto;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.mq.MyMessageProducer;
import com.jgh.aianalysis.service.OrderService;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.entity.Chart;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;

import static com.jgh.aianalysis.mq.MQConstant.*;


@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Resource
    private OrderService orderService;

    @Resource
    private MyMessageProducer myMessageProducer;

    @PostMapping("/create")
    public BaseResponse<Boolean> createOrder(@RequestBody OrderPayDto orderPayDto, HttpServletRequest request) {
        log.info("创建订单");
        String userIdString = request.getHeader("userId");
        if (userIdString == null) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }
        Long userId = Long.parseLong(userIdString);

        Order order = new Order();
        order.setId(orderPayDto.getId());
        order.setUserId(userId);
        order.setMoney(orderPayDto.getMoney());
        order.setPaymentMethod(orderPayDto.getPaymentMethod());
        order.setStatus(PayStatusEnum.AWAIT_PAY.getStatus());
        order.setDescription(PayStatusEnum.AWAIT_PAY.getDesc());

        boolean save = orderService.save(order);
        if (!save) {
            log.error("创建订单失败");
            throw new BusinessException("创建订单失败");
        }

        // todo  向消费者发送消息，设置15分钟过期，然后由死信队列去接受
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("orderId", orderPayDto.getId());
        objectObjectHashMap.put("userId", userId);
        myMessageProducer.sendMessage(ORDER_EXCHANGE_NAME, ORDER_ROUTING_KEY, objectObjectHashMap);
        return BaseResponse.success(true);
    }

    @PostMapping("/detail")
    public BaseResponse<Order> getOrderDetail(@RequestBody OrderDetailDto orderDetailDto, HttpServletRequest request) {
        log.info("获取订单详情");
        String userIdString = request.getHeader("userId");
        if (userIdString == null) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }
        Long userId = Long.parseLong(userIdString);

        QueryWrapper<Order> wrapper = new QueryWrapper<>();

        wrapper.eq("id", orderDetailDto.getId())
                .eq("userId", userId);

        Order order = orderService.getOne(wrapper);

        if (order == null) {
            log.error("订单不存在");
            throw new BusinessException("订单不存在");
        }

        return BaseResponse.success(order);
    }

    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> getOrderList(@RequestBody OrderPageDto orderPageDto, HttpServletRequest request) {
        log.info("分页获取订单列表");
        String userIdString = request.getHeader("userId");
        if (userIdString == null) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }
        long current = orderPageDto.getCurrent();
        long size = orderPageDto.getPageSize();
        Page<Chart> chartPage = orderService.page(new Page<>(current, size),
                getQueryWrapper(orderPageDto));
        return BaseResponse.success(chartPage);

    }

    private QueryWrapper getQueryWrapper(OrderPageDto orderPageDto) {

        Long id = orderPageDto.getId();
        Long userId = orderPageDto.getUserId();
        Double money = orderPageDto.getMoney();
        String paymentMethod = orderPageDto.getPaymentMethod();
        Integer status = orderPageDto.getStatus();
        String description = orderPageDto.getDescription();

        QueryWrapper<Chart> wrapper = new QueryWrapper<>();
        wrapper.eq(id != null && id > 0, "id", id)
                .eq(userId != null, "userId", userId)
                .eq(money != null, "money", money)
                .eq(paymentMethod != null, "paymentMethod", paymentMethod)
                .eq(status != null, "status", status)
                .like(description != null, "description", description)
                .orderBy(true, false, "createTime");
        return wrapper;


    }
}

