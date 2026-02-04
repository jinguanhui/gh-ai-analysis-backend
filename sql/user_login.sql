-- 用户登录信息表
create table if not exists user_login
(
    id         bigint auto_increment comment 'id' primary key,
    description  varchar(128)                       null comment '登录描述',
    loginPath varchar(128)                       null comment '登录IP',
    region varchar(128)                       null comment '登录IP所属地',
    userId     bigint                             null comment '创建用户 id',
    loginStatus     bigint                             null comment '登录状态 0-成功 1-失败',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
) comment '用户登录信息表' collate = utf8mb4_unicode_ci;
