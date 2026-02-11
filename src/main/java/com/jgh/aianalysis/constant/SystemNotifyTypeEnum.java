package com.jgh.aianalysis.constant;

import lombok.Getter;

/**
 * 支付状态常量
 *
 * @author: 光吾
 */
@Getter
public enum SystemNotifyTypeEnum {

    // 通知类型: PAY_SUCCESS(支付成功), AI_ANALYSIS_COMPLETE(AI分析完成), AI_ANALYSIS_FAILED(AI分析失败), ORDER_CANCELLED(订单取消)等

    PAY_SUCCESS("支付成功"),
    AI_ANALYSIS_COMPLETE("AI分析完成"),
    AI_ANALYSIS_FAILED("AI分析失败"),
    ORDER_CANCELLED("订单取消"),
    PAY_FAILED("支付失败"),
    REFUND_SUCCESS("退款成功"),
    REFUND_FAILED("退款失败"),
    ORDER_TYPE("订单类型"),
    ANALYSIS_TYPE("AI分析类型"),
    SYSTEM_TYPE("系统类型");

    private final String notifyType;


    SystemNotifyTypeEnum(String notifyType) {
        this.notifyType = notifyType;
    }
}