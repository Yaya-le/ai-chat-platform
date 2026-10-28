package com.zykj.ai_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_chat_message")
public class ChatMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer sessionId;

    private String content;

    private Integer role;

    private Date createTime;

    private Integer deleted;
}
