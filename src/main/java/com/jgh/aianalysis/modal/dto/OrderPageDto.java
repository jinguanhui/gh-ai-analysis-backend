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
     * 创建用户 id
     */
    private Long userId;

    /**
     * 0-待支付，1-已支付，2-退款，3-已取消
     */
    private Integer status;


}