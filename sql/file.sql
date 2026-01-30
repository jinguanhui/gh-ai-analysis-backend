-- 图表信息表
create table if not exists gh_file
(
    id        bigint auto_increment comment 'id' primary key,
    chartId   bigint            null comment '图表 id',
    fileName     varchar(128)                       null comment '文件名称',
    fileExcel longblob          NOT NULL COMMENT 'excel 文件',
    isDelete  tinyint default 0 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;
