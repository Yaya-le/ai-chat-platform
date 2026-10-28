package com.zykj.ai_backend.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zykj.ai_backend.entity.ChatSession;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
