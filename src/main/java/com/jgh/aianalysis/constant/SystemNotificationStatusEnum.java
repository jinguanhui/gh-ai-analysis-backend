package com.jgh.aianalysis.constant;

import lombok.Getter;

/**
 * 支付状态常量
 * @author: 光吾
 */
@Getter
public enum SystemNotificationStatusEnum {
    READ(1, "已读"),
    UNREAD(0, "未读");

    private final Integer status;
    private final String desc;


    SystemNotificationStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}