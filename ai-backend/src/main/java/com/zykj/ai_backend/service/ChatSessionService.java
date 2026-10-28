package com.zykj.ai_backend.service;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zykj.ai_backend.entity.ChatSession;
import com.zykj.ai_backend.mapper.ChatSessionMapper;

import jakarta.annotation.Resource;

@Service
public class ChatSessionService {

    @Resource
    private ChatSessionMapper chatSessionMapper;

    // 创建会话
    public ChatSession createSession(Integer userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setCreateTime(new Date());
        session.setDeleted(0);
        chatSessionMapper.insert(session);
        return getSessionDetail(session.getId());
    }

    // 查询会话列表
    public List<ChatSession> getSessionList(Integer userId) {
        return chatSessionMapper.selectList(
            new QueryWrapper<ChatSession>()
                .eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("update_time")
        );
    }

    // 获取会话详情
    public ChatSession getSessionDetail(Integer sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }

    // 更新会话（时间、标题）
    public void updateSession(Integer sessionId, String title) {
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        if(title!=null && title.length()>0) {
            //标题太长截断
            if(title.length()>20)
                title = title.substring(0, 20);
            session.setTitle(title);
        }
        session.setUpdateTime(new Date());
        chatSessionMapper.updateById(session);
    }

    // 删除会话(逻辑删除)
    public void deleteSession(Integer sessionId) {
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setDeleted(1);
        chatSessionMapper.updateById(session);
    }
}
