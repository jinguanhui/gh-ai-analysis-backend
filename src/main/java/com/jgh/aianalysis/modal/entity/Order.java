package com.jgh.aianalysis.modal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

import lombok.Data;

/**
 * 图表信息表
 *
 * @TableName order
 */
@TableName(value = "orders")
@Data
public class Order {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 支付金额
     */
    private Double money;

    /**
     * 支付方式
     */
    private String paymentMethod;
    private String refundNo;

    /**
     * 0-待支付，1-已支付，2-退款，3-已取消
     */
    private Integer status;

    /**
     * 订单描述
     */
    private String description;
    private String alipayTradeNo;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private Date payTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}