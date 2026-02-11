-- 图表信息表
-- 系统通知表
CREATE TABLE IF NOT EXISTS system_notification
(
    id                BIGINT AUTO_INCREMENT COMMENT '主键ID',
    user_id           BIGINT                             NOT NULL COMMENT '接收通知的用户ID',
    notification_type VARCHAR(64)                        NOT NULL COMMENT '通知类型: PAY_SUCCESS(支付成功), AI_ANALYSIS_COMPLETE(AI分析完成), AI_ANALYSIS_FAILED(AI分析失败), ORDER_CANCELLED(订单取消)等',
    title             VARCHAR(255)                       NOT NULL COMMENT '通知标题',
    content           TEXT                               NOT NULL COMMENT '通知内容',
    related_id        BIGINT COMMENT '关联ID: 支付订单ID或图表ID',
    related_type      VARCHAR(64) COMMENT '关联类型: ORDER(订单), CHART(图表)',
    isRead            TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否已读: 0-未读, 1-已读',
    createTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete          TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_related_id (related_id)
) COMMENT ='系统通知表' COLLATE = utf8mb4_unicode_ci;