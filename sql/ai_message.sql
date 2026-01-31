DROP TABLE IF EXISTS `chat_message`;
create table chat_message
(
    id              bigint unsigned auto_increment comment '主键ID'
        primary key,
    conversationId varchar(64)                          not null comment '会话ID',
    messageType    varchar(20)                          not null comment '消息类型',
    content         text                                 not null comment '消息内容',
    metadata        text                                 not null comment '元数据',
    createTime     datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint(1) default 0                 not null comment '是否删除 0-未删除 1-已删除'
)
    comment '聊天消息表';

create index idx_conversation_id
    on chat_message (conversationId);

