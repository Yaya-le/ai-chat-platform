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

-- 4. 创建会话表
CREATE TABLE IF NOT EXISTS t_chat_session (
    id INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    title VARCHAR(255) COMMENT '会话标题',
    user_id INT(11) COMMENT '用户ID',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) COMMENT '会话表';

-- 5. 创建消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id INT(11) PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id INT(11) COMMENT '会话ID',
    content TEXT COMMENT '消息内容',
    role INT(11) COMMENT '角色：0-用户，1-AI',
    create_time DATETIME COMMENT '创建时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) COMMENT '消息表';

-- 6. 初始化一条测试数据
INSERT INTO t_user(username, password, nickname) VALUES ('jack', '123456', '小杰');

-- 7. 验证
SELECT * FROM t_user;
