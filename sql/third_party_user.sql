-- 第三方登录用户信息表
create table if not exists third_party_user
(
    id                 bigint auto_increment comment 'id' primary key,
    `provider_type`    varchar(50)                        NOT NULL COMMENT '第三方登录类型: phone-手机, email-邮箱, wechat-微信, alipay-支付宝等',
    `provider_id`      varchar(255)                       NOT NULL COMMENT '第三方平台唯一标识符(如微信openid、手机号、邮箱等)',
    `provider_account` varchar(255) COMMENT '第三方账号(如微信号、手机号、邮箱地址等)',
    `raw_data`         json COMMENT '原始数据(JSON格式存储第三方返回的用户信息)',
    userId             bigint                             null comment '创建用户 id',
    createTime         datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete           tinyint  default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;
