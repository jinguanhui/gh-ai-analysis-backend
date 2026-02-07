-- 图表信息表
create table if not exists `order`
(
    id              bigint auto_increment comment 'id' primary key,
    userId          bigint                             null comment '创建用户 id',
    interfaceInfoId bigint                             null comment '接口 id',
    money           double                             null comment '支付金额',
    paymentMethod   varchar(128)                       null comment '支付方式',
    status          tinyint  default 0                 not null comment '是否支付',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;
