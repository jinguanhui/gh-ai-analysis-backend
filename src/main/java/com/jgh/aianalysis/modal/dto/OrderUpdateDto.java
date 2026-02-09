package com.jgh.aianalysis.modal.dto;

import lombok.Data;

/**
 * 图表信息表
 *
 * @TableName order
 */
@Data
public class OrderUpdateDto {
    /**
     * id
     */
    private Long id;

    private Integer status;

    private String description;
}