package com.jgh.aianalysis.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.jgh.aianalysis.modal.dto.OrderPayDto;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.AlipayService;
import com.jgh.ghcommon.common.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        //验签
        boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType());  //调用SDK验证签名
        if (signVerified) {
            log.info("收到支付宝发送的支付结果通知");
            logTransactionDetails(params);
            String out_trade_no = request.getParameter("out_trade_no");
            log.info("交易流水号：{}", out_trade_no);
            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes(), StandardCharsets.UTF_8);
            //交易成功
            switch (trade_status) {
                case "TRADE_SUCCESS":
                    //支付成功的业务逻辑，比如落库，开vip权限等
                    log.info("订单：{} 交易成功", out_trade_no);
                    break;
                case "TRADE_FINISHED":
                    log.info("交易结束，不可退款");
                    //其余业务逻辑
                    break;
                case "TRADE_CLOSED":
                    log.info("超时未支付，交易已关闭，或支付完成后全额退款");
                    //其余业务逻辑
                    break;
                case "WAIT_BUYER_PAY":
                    log.info("交易创建，等待买家付款");
                    //其余业务逻辑
                    break;
            }
            response.getWriter().write("success");   //返回success给支付宝，表示消息我已收到，不用重调

        } else {
            response.getWriter().write("fail");   ///返回fail给支付宝，表示消息我没收到，请重试
        }
    }
    /**
     * 记录交易详情的方法
     *
     * @param params
     */
    private void logTransactionDetails(Map<String, String> params) {
        log.info("交易名称: " + params.get("subject"));
        log.info("交易状态: " + params.get("trade_status"));
        log.info("支付宝交易凭证号: " + params.get("trade_no"));
        log.info("商户订单号: " + params.get("out_trade_no"));
        log.info("交易金额: " + params.get("total_amount"));
        log.info("买家在支付宝唯一id: " + params.get("buyer_id"));
        log.info("买家付款时间: " + params.get("gmt_payment"));
        log.info("买家付款金额: " + params.get("buyer_pay_amount"));
    }
}