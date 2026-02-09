package com.jgh.aianalysis.service.impl;

import cn.hutool.json.JSONUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
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
}
