CREATE TABLE `access_key`
(
    `id`           bigint                                                        NOT NULL AUTO_INCREMENT,
    `aesKey`     varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'aesKey',
    `publicKey`  varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'publicKey',
    `privateKey` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'privateKey',
    `userId`       bigint                                                        NOT NULL COMMENT '用户id',
    `status`       tinyint                                                       NOT NULL COMMENT '状态 0-正常 1-禁用',
    `lastUsedTime` datetime                                                      NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '最后使用时间',
    `expireTime`   datetime                                                      NULL     COMMENT '过期时间',
    `createTime`   datetime                                                      NULL     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `isDelete`     tinyint                                                       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 6
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户密钥表'
  ROW_FORMAT = Dynamic;
