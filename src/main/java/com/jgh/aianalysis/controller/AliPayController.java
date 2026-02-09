package com.jgh.aianalysis.controller;

import com.alipay.api.AlipayConfig;
import com.alipay.api.internal.util.AlipaySignature;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jgh.aianalysis.constant.PayStatusEnum;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.modal.dto.AlipayQueryDto;
import com.jgh.aianalysis.modal.dto.OrderPayDto;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.AlipayService;
import com.jgh.aianalysis.service.OrderService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.RedisUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 支付宝接口
 */
@RestController
@RequestMapping("/alipay")
@Slf4j
public class AliPayController {

    @Resource
    private AlipayService alipayService;

    @Resource
    private AlipayConfig alipayConfig;

    @Resource
    private OrderService orderService;

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 收银台点击结账
     * 发起下单请求
     */
    @PostMapping("/pay")
    public BaseResponse<String> pay(@RequestBody OrderPayDto orderPayDto, HttpServletRequest request, HttpServletResponse response) {
        log.info("开始进行支付宝支付");
        return BaseResponse.success(alipayService.payWithCode(orderPayDto, request, response));
    }

    /**
     * 给支付宝的回调接口
     */
    @PostMapping("/notify")
    public void notify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> params = new HashMap<>();
        //获取支付宝POST过来反馈信息，将异步通知中收到的待验证所有参数都存放到map中
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (String name : parameterMap.keySet()) {
            String[] values = parameterMap.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决
            valueStr = new String(valueStr.getBytes(), StandardCharsets.UTF_8);
            params.put(name, valueStr);
        }


        String subject = params.get("subject");
        String tradeStatus = params.get("trade_status");
        String tradeNo = params.get("trade_no");
        String[] split = params.get("out_trade_no").split("\\|");
        String outTradeNo = split[0];
        String userId = split[1];
        String totalAmount = params.get("total_amount");
        String buyerId = params.get("buyer_id");
        String gmtPayment = params.get("gmt_payment");
        String buyerPayAmount = params.get("buyer_pay_amount");
        log.info("系统用户ID ：{}", userId);


        //验签
        boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType());  //调用SDK验证签名
        log.info("收到支付宝发送的支付结果通知");
        logTransactionDetails(subject, tradeStatus, tradeNo, outTradeNo, totalAmount, buyerId, gmtPayment, buyerPayAmount);
        Order order = new Order();
        if (signVerified) {
            //交易成功
            handleCheckTradeStatus(tradeStatus, outTradeNo, order, tradeNo, userId);

            response.getWriter().write("success");   //返回success给支付宝，表示消息我已收到，不用重调

        } else {
            log.info("收到支付宝发送的支付结果通知验签失败,将主动查询支付状态");
//            response.getWriter().write("failure");   ///返回failure给支付宝，表示消息我没收到，请重试

            handleCheckTradeStatus(tradeStatus, outTradeNo, order, tradeNo, userId);

        }
    }

    /**
     * 主动查询支付宝支付结果
     *
     * @param alipayQueryDto
     * @param request
     * @return
     */
    @PostMapping("/check_payInfo")
    public BaseResponse checkTradeStatus(@RequestBody AlipayQueryDto alipayQueryDto, HttpServletRequest request) {
        String outTradeNo = alipayQueryDto.getOutTradeNo();
        alipayService.checkTradeStatus(outTradeNo);
        return BaseResponse.success();
    }

    private void handleCheckTradeStatus(String tradeStatus, String outTradeNo, Order order, String tradeNo, String userId) {
        // 使用分布式锁确保同一订单不会并发处理
        // 1.获取一把锁，只要锁的名字一样，就是同一把锁
        RLock rLock = redissonClient.getLock(outTradeNo + "pay");
        // 2.加锁
        rLock.lock(); // 阻塞式等待
        try {
            switch (tradeStatus) {
                case "TRADE_SUCCESS":
                    //支付成功的业务逻辑，比如落库，开vip权限等
                    log.info("订单：{} 交易成功", outTradeNo);

                    Boolean execute = transactionTemplate.execute(transactionStatus -> {
                        handleUpdateOrder(order, outTradeNo, PayStatusEnum.SUCCESS.getStatus(), PayStatusEnum.SUCCESS.getDesc(), tradeNo);
                        handleUpdateUser(userId);
                        return true;
                    });
                    if (!execute) {
                        log.error("更新订单失败");
                        throw new BusinessException("更新订单失败");
                    }
                    break;
                case "TRADE_FINISHED":
                    handleUpdateOrder(order, outTradeNo, PayStatusEnum.FINISHED.getStatus(), PayStatusEnum.FINISHED.getDesc(), tradeNo);

                    //其余业务逻辑
                    break;
                case "TRADE_CLOSED":
                    log.info("超时未支付，交易已关闭，或支付完成后全额退款");
                    handleUpdateOrder(order, outTradeNo, PayStatusEnum.CANCEL.getStatus(), "超时未支付，交易已关闭", tradeNo);
                    //其余业务逻辑
                    break;
                case "WAIT_BUYER_PAY":
                    log.info("交易创建，等待买家付款");
                    //其余业务逻辑
                    handleUpdateOrder(order, outTradeNo, PayStatusEnum.AWAIT_PAY.getStatus(), PayStatusEnum.AWAIT_PAY.getDesc(), tradeNo);
                    break;
            }
        } catch (TransactionException e) {
            log.error("分布式事务异常", e);
            throw new RuntimeException(e);
        } catch (BusinessException e) {
            log.error("业务异常", e);
            throw new RuntimeException(e);
        } finally {
            rLock.unlock();
            log.info("分布式锁释放成功");

        }
    }

    private void handleUpdateUser(String userId) {
        log.info("用户：{} 添加调用次数", userId);
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", Long.parseLong(userId));
        wrapper.setSql("invokeCount = invokeCount + 100");

        boolean update = userService.update(wrapper);
        if (!update) {
            log.error("用户：{} 添加调用次数失败", userId);
            throw new BusinessException("用户：{} 添加调用次数失败");
        }
    }

    private void handleUpdateOrder(Order order, String outTradeNo, Integer tradeStatus, String desc, String tradeNo) {
        log.info("更新订单");
        order.setId(Long.parseLong(outTradeNo));
        order.setStatus(tradeStatus);
        order.setDescription(desc);
        order.setUpdateTime(new Date());
        order.setPayTime(new Date());
        order.setAlipayTradeNo(tradeNo);

        boolean b = orderService.updateById(order);
        if (!b) {
            log.error("更新订单失败");
            throw new BusinessException("更新订单失败");
        }
    }

    /**
     * 记录交易信息
     *
     * @param subject
     * @param tradeStatus
     * @param tradeNo
     * @param outTradeNo
     * @param totalAmount
     * @param buyerId
     * @param gmtPayment
     * @param buyerPayAmount
     */
    private void logTransactionDetails(String subject, String tradeStatus, String tradeNo, String outTradeNo, String totalAmount, String buyerId, String gmtPayment, String buyerPayAmount) {
        /**
         * 交易名称: 10元续费100次AI分析
         * 交易状态: TRADE_SUCCESS
         * 支付宝交易凭证号: 2026020922001425320508221254
         * 商户订单号: 944165562627
         * 交易金额: 10.00
         * 买家在支付宝唯一id: 2088722094925327
         * 买家付款时间: 2026-02-09 14:20:24
         * 买家付款金额: 10.00
         */
        log.info("交易名称: " + subject);
        log.info("交易状态: " + tradeStatus);
        log.info("支付宝交易凭证号: " + tradeNo);
        log.info("商户订单号: " + outTradeNo);
        log.info("交易金额: " + totalAmount);
        log.info("买家在支付宝唯一id: " + buyerId);
        log.info("买家付款时间: " + gmtPayment);
        log.info("买家付款金额: " + buyerPayAmount);
    }

}