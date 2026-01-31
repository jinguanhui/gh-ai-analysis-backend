package com.jgh.aianalysis.manager.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除 0-未删除 1-已删除
     */
    @TableLogic
    private Integer isDelete;
}