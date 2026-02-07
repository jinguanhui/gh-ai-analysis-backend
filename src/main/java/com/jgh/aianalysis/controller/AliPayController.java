package com.jgh.aianalysis.controller;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.jgh.aianalysis.modal.entity.Order;
import com.jgh.aianalysis.service.AlipayService;
import com.jgh.ghcommon.common.BaseResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping("/pay")
    public BaseResponse<String> pay(HttpServletRequest request, HttpServletResponse response) {
        log.info("开始进行支付宝支付");
        return BaseResponse.success(alipayService.payWithCode(request, response));
    }

    /**
     * 给支付宝的回调接口
     */
    @PostMapping("/notify")
    public BaseResponse<Boolean> notify(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
            valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(name, valueStr);
        }
        //验签
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        Boolean  signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getAlipayPublicKey(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType());  //调用SDK验证签名
        if (signVerified) {
            log.info("收到支付宝发送的支付结果通知");
            String out_trade_no = request.getParameter("out_trade_no");
            log.info("交易流水号：{}", out_trade_no);
            //交易状态
            String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");
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
        return BaseResponse.success(true);
    }
//    @GetMapping(value = "/pay", produces = "text/html")
//    public BaseResponse<String> pay(@RequestParam long id) throws AlipayApiException {
//        // 创建订单对象并设置属性
//        Order order = createOrder(id);
//
//        // 调用支付宝支付模板进行支付
//        return BaseResponse.success(aliPayConfig.pay(order));
//    }
//

    /**
     * 创建订单的方法
     * 因为是测试并没有创建数据库表，所以直接new了一个对象出来
     *
     * @param id
     * @return
     */
    private Order createOrder(long id) {
        Order order = new Order();
        order.setId(id);
        return order;
    }

//    @PostMapping("/notify")  // 注意这里必须是POST接口
//    public BaseResponse<String> payNotify(HttpServletRequest request) throws Exception {
//        // 检查交易状态是否为成功
//        if (!"TRADE_SUCCESS".equals(request.getParameter("trade_status"))) {
//            return BaseResponse.success("failure"); // 如果状态不是成功，则返回失败
//        }
//
//        System.out.println("=========支付宝异步回调========");
//
//        // 创建一个存储请求参数的Map
//        Map<String, String> params = getRequestParams(request);
//
//        // 提取支付信息
//        String tradeNo = params.get("out_trade_no");    // 商户订单号
//        String gmtPayment = params.get("gmt_payment");  // 付款时间
//        String alipayTradeNo = params.get("trade_no");  // 支付宝交易号
//
//        // 验证支付宝返回的签名
//        if (verifySignature(params)) {
//            logTransactionDetails(params); // 记录交易详情
//            // 更新订单状态的逻辑可以在这里添加
//        }
//
//        return BaseResponse.success("success"); // 返回成功响应给支付宝
//    }

    /**
     * 提取请求参数的方法
     *
     * @param request
     * @return
     */
    private Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            params.put(name, request.getParameter(name));
        }
        return params;
    }

    /**
     * 验证签名的方法
     *
     * @param params
     * @return
     * @throws Exception
     */
    private boolean verifySignature(Map<String, String> params) throws Exception {
//        return Factory.Payment.Common().verifyNotify(params);
        return false;
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