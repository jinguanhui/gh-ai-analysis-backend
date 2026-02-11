package com.jgh.aianalysis.service.impl;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.domain.*;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.jgh.aianalysis.constant.PayStatusEnum;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.modal.dto.OrderPayDto;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.AlipayService;
import com.jgh.aianalysis.service.OrderService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@Slf4j
public class AlipayServiceImpl implements AlipayService {

    @Resource
    private AlipayConfig alipayConfig;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @Value("${alipay.returnUrl}")
    private String returnUrl;

    @Resource
    private OrderService  orderService;

    @Override
    public String payWithCode(OrderPayDto orderPayDto, HttpServletRequest request, HttpServletResponse response) {
        AlipayClient alipayClient = null;
        try {
            // 1. 创建Client，通用SDK提供的Client，负责调用支付宝的API
            alipayClient = new DefaultAlipayClient(alipayConfig);


            AlipayTradePagePayRequest alipayTradePagePayRequest = new AlipayTradePagePayRequest();

            String userIdHeader = request.getHeader("UserId");
            if (userIdHeader == null) {
                log.error("用户不存在！！！");
                throw new BusinessException("用户不存在");
            }

            alipayTradePagePayRequest.setNotifyUrl(notifyUrl);
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();

            // 设置商户订单号
            model.setOutTradeNo(orderPayDto.getId() + "|" + userIdHeader);
//            model.setOutTradeNo(String.valueOf(orderPayDto.getId()));

            // 设置订单总金额
            model.setTotalAmount(String.valueOf(orderPayDto.getMoney()));

            // 设置订单标题
            model.setSubject("10元续费100次AI分析");

            // 设置产品码
            model.setProductCode("FAST_INSTANT_TRADE_PAY");

            alipayTradePagePayRequest.setBizModel(model);

            alipayTradePagePayRequest.setReturnUrl(returnUrl);
            alipayTradePagePayRequest.setNotifyUrl(notifyUrl);

            AlipayTradePagePayResponse alipayTradePagePayResponse = alipayClient.pageExecute(alipayTradePagePayRequest);
            System.out.println(JSONUtil.toJsonStr(alipayTradePagePayResponse));
            // 如果需要返回GET请求，请使用
            // AlipayTradePagePayResponse alipayTradePagePayResponse = alipayClient.pageExecute(request, "GET");
            String pageRedirectionData = alipayTradePagePayResponse.getBody();
            System.out.println(pageRedirectionData);

            if (alipayTradePagePayResponse.isSuccess()) {
                System.out.println("调用成功");
            } else {
                System.out.println("调用失败");
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                 String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(alipayTradePagePayResponse);
                 System.out.println(diagnosisUrl);
            }
            return pageRedirectionData;
        } catch (AlipayApiException e) {
            log.error("支付宝支付失败", e);
            throw new BusinessException("支付宝支付失败");
        }
    }

    @Override
    public void checkTradeStatus(String outTradeNo) {
        AlipayTradeQueryResponse response = null;
        Order order = orderService.getById(outTradeNo);
        String tradeNo = order.getAlipayTradeNo();

        try {
            // 初始化SDK
            AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);

            // 构造请求参数以调用接口
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();

            // 设置订单支付时传入的商户订单号
            model.setOutTradeNo(outTradeNo);

            // 设置支付宝交易号
            model.setTradeNo(tradeNo);

            request.setBizModel(model);


            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            log.error("支付宝查询失败", e);
            throw new BusinessException("支付宝查询失败");
        }
        System.out.println(response.getBody());

        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
            // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
            // String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            // System.out.println(diagnosisUrl);
        }
    }

    @Override
    public void refund(String outTradeNo) {
        Order order = orderService.getById(outTradeNo);
        String refundNo = UUID.randomUUID().toString();
        if (order == null) {
            log.error("订单不存在");
            throw new BusinessException("订单不存在");
        }

        if (order.getStatus() != PayStatusEnum.SUCCESS.getStatus()) {
            log.error("订单未支付,不可退换");
            throw new BusinessException("订单未支付,不可退换");
        }
        String tradeNo = order.getAlipayTradeNo();
        // 初始化SDK
        AlipayClient alipayClient = null;
        try {
            alipayClient = new DefaultAlipayClient(alipayConfig);
            // 构造请求参数以调用接口
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();

            // 设置商户订单号
            model.setOutTradeNo(outTradeNo);

            // 设置支付宝交易号
            model.setTradeNo(tradeNo);

            // 设置退款金额
            model.setRefundAmount(String.valueOf(order.getMoney()));

            // 设置退款原因说明
            model.setRefundReason("用户正常退款");

            // 设置退款请求号
            model.setOutRequestNo(refundNo);

            request.setBizModel(model);
            // 第三方代调用模式下请设置app_auth_token
            // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            System.out.println(response.getBody());

            if (response.isSuccess()) {
                System.out.println("调用成功");
                order.setStatus(PayStatusEnum.REFUND.getStatus());
                order.setDescription(PayStatusEnum.REFUND.getDesc());
                order.setRefundNo(refundNo);

                boolean b = orderService.updateById(order);
                if (!b) {
                    log.error("订单更新失败");
                    throw new BusinessException("订单更新失败");
                }
            } else {
                System.out.println("调用失败");
                // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
                 String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
                 System.out.println(diagnosisUrl);
                 throw new BusinessException("支付宝退款失败");
            }
        } catch (AlipayApiException e) {
            log.error("支付宝退款失败", e);
            throw new BusinessException( "支付宝退款失败");
        }


    }
}
