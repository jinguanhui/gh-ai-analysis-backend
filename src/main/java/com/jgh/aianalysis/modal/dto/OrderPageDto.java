package com.jgh.aianalysis.modal.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.jgh.ghcommon.common.PageRequest;
import lombok.Data;

import java.util.Date;

/**
 * 图表信息表
 *
 * @TableName order
 */
@Data
public class OrderPageDto extends PageRequest {
    /**
     * id
     */
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

    /**
     * 0-待支付，1-已支付，2-退款，3-已取消
     */
    private Integer status;

    /**
     * 订单描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private Date payTime;

}