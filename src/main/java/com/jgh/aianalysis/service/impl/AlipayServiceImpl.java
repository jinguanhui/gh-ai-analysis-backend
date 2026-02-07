package com.jgh.aianalysis.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.diagnosis.DiagnosisUtils;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.modal.dto.OrderPayDto;
import com.jgh.aianalysis.service.AlipayService;
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

    @Override
    public String payWithCode(OrderPayDto orderPayDto, HttpServletRequest request, HttpServletResponse response) {
        AlipayClient alipayClient = null;
        try {
            // 1. 创建Client，通用SDK提供的Client，负责调用支付宝的API
            alipayClient = new DefaultAlipayClient(alipayConfig);


            AlipayTradePagePayRequest alipayTradePagePayRequest = new AlipayTradePagePayRequest();

//            alipayTradePagePayRequest.setBizContent("  {" +
//                    "    \"subject\":\"jadgiangadignadgadaga\"," +
//                    "    \"total_amount\":\"11111\"," +
//                    "    \"body\":\"asdfd\"," +
//                    "    \"out_trade_no\":\"12354\"," +
//                    "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                    " }");

            // 第三方代调用模式下请设置app_auth_token
            // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");
            // 2. 创建 Request并设置Request参数

            String userIdHeader = request.getHeader("UserId");
            if (userIdHeader == null) {
                log.error("用户不存在！！！");
                throw new BusinessException("用户不存在");
            }

            long userId = Long.parseLong(userIdHeader);

            alipayTradePagePayRequest.setNotifyUrl(notifyUrl);
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderPayDto.getId());  // 我们自己生成的订单编号
            bizContent.put("total_amount", orderPayDto.getMoney()); // 订单的总金额
            bizContent.put("user_id", userId); // 订单的总金额
            bizContent.put("subject", "10元续费100次AI分析");   // 支付的名称
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");  // 固定配置
            alipayTradePagePayRequest.setBizContent(bizContent.toString());

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
}
