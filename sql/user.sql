/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80031 (8.0.31)
 Source Host           : localhost:3306
 Source Schema         : user_center

 Target Server Type    : MySQL
 Target Server Version : 80031 (8.0.31)
 File Encoding         : 65001

 Date: 30/12/2025 15:21:59
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`           bigint                                                        NOT NULL AUTO_INCREMENT,
    `username`     varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
    `userAccount`  varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账号',
    `avatarUrl`    varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
    `gender`       tinyint NULL DEFAULT NULL COMMENT '性别',
    `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
    `accessKey` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'accessKey',
    `secretKey` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'secretKey',
    `salt` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '盐值',
    `email`        varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
    `userStatus`   int NULL DEFAULT 0 COMMENT '状态 0-正常',
    `phone`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '电话',
    `createTime`   datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     tinyint                                                       NOT NULL DEFAULT 0 COMMENT '是否删除',
    `invokeCount`     tinyint                                                       NOT NULL DEFAULT 0 COMMENT '调用次数',
    `userRole`     int                                                           NOT NULL DEFAULT 0 COMMENT '用户权限 0-普通用户 1-管理员',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user`
VALUES (2, NULL, 'dogyupi', '', NULL, 'b0dd3697a192885d7c055db46155b26a', NULL, 0, NULL, '2025-12-26 20:54:07',
        '2025-12-28 21:29:01', 1, 0);
INSERT INTO `user`
VALUES (3, NULL, 'yupi', '', NULL, 'b0dd3697a192885d7c055db46155b26a', NULL, 0, NULL, '2025-12-26 20:55:57',
        '2025-12-28 21:29:01', 0, 0);
INSERT INTO `user`
VALUES (4, NULL, 'jinguanhui', '', NULL, 'e3c51c21f80378a3ed7ca7f026f6d2db', NULL, 0, NULL, '2025-12-27 18:36:32',
        '2025-12-28 21:29:01', 1, 0);
INSERT INTO `user`
VALUES (5, NULL, 'jinguanhui', '', NULL, 'e3c51c21f80378a3ed7ca7f026f6d2db', NULL, 0, NULL, '2025-12-27 18:43:56',
        '2025-12-28 21:29:01', 0, 1);

SET
FOREIGN_KEY_CHECKS = 1;
