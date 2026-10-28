package com.zykj.ai_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zykj.ai_backend.ai.AIEngineClient;
import com.zykj.ai_backend.common.Result;
import com.zykj.ai_backend.entity.ChatMessage;
import com.zykj.ai_backend.entity.ChatSession;
import com.zykj.ai_backend.mapper.ChatMessageMapper;
import com.zykj.ai_backend.service.ChatSessionService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Resource
    private ChatMessageMapper chatMessageMapper;
    @Resource
    private ChatSessionService chatSessionService;
    @Resource
    private AIEngineClient aiEngineClient;

    private Integer getUserId(HttpServletRequest request) {
        return Integer.valueOf(request.getAttribute("userId").toString());
    }

    // 获取会话的消息列表
    @RequestMapping("/list")
    public Result<List<ChatMessage>> getMessageList(@RequestParam Integer sessionId) {
        ChatSession cs = chatSessionService.getSessionDetail(sessionId);
        if (cs == null) {
            return Result.fail(400, "会话不存在");
        }
        List<ChatMessage> list = chatMessageMapper.selectList(
            new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId)
                .eq("deleted", 0));
        return Result.ok(200, "获取会话消息列表成功", list);
    }

    // 聊天
    @RequestMapping("/send")
    public Result<String> send(HttpServletRequest request, @RequestParam String message, @RequestParam Integer sessionId) {
        int userId = getUserId(request);
        // 1、建/取会话
        ChatSession cs = chatSessionService.getSessionDetail(sessionId);
        if (cs == null) {
            cs = chatSessionService.createSession(userId, message);
        }

        // 2、保存用户消息
        ChatMessage cm = new ChatMessage();
        cm.setSessionId(sessionId);
        cm.setContent(message);
        cm.setCreateTime(new java.util.Date());
        cm.setDeleted(0);
        cm.setRole(0);
        chatMessageMapper.insert(cm);

        // 3、生成AI回复内容
        String aiMessage = aiEngineClient.chatComplete(message, null, List.of(), "");
        System.out.println("aiMessage:" + aiMessage);

        // 4、保存AI消息
        ChatMessage cm2 = new ChatMessage();
        cm2.setSessionId(sessionId);
        cm2.setContent(aiMessage);
        cm2.setCreateTime(new java.util.Date());
        cm2.setDeleted(0);
        cm2.setRole(1);
        chatMessageMapper.insert(cm2);

        // 5、更新会话时间
        chatSessionService.updateSession(sessionId, null);

        return Result.ok(200, "发送成功", null);
    }
}
