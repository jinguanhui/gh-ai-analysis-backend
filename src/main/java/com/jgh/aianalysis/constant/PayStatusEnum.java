package com.jgh.aianalysis.constant;

import lombok.Getter;
/**
 * 支付状态常量
 * @author: 光吾
 */
@Getter
public enum PayStatusEnum {
    AWAIT_PAY(0, "待支付"),
    SUCCESS(1, "支付成功"),
    REFUND(2, "已退款"),
    CANCEL(3, "已取消"),
    FINISHED(4, "已完成");

    private final Integer status;
    private final String desc;

    
    PayStatusEnum(Integer status,String desc) {
        this.status = status;
        this.desc = desc;
    }
}