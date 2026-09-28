-- =============================================
-- ai-chat-platform1 数据库初始化脚本
-- =============================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ai_chat1 DEFAULT CHARACTER SET = 'utf8mb4';

-- 2. 使用数据库
USE ai_chat1;

-- 3. 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名（唯一）',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(255) NOT NULL COMMENT '昵称',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) COMMENT '用户表';

-- 4. 初始化一条测试数据
INSERT INTO t_user(username, password, nickname) VALUES ('jack', '123456', '小杰');

-- 5. 验证
SELECT * FROM t_user;
