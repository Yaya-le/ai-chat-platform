package com.zykj.ai_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zykj.ai_backend.common.Result;
import com.zykj.ai_backend.entity.ChatSession;
import com.zykj.ai_backend.service.ChatSessionService;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/session")
public class ChatSessionController {

    @Resource
    private ChatSessionService chatSessionService;

    private Integer getUserId(HttpServletRequest request) {
        return Integer.valueOf(request.getAttribute("userId").toString());
    }

    @RequestMapping("/create")
    public Result<ChatSession> createSession(HttpServletRequest request, @RequestParam String title) {
        Integer userId = getUserId(request);
        ChatSession cs = chatSessionService.createSession(userId, title);
        return Result.ok(200, "创建成功", cs);
    }

    @RequestMapping("/list")
    public Result<List<ChatSession>> getSessionList(HttpServletRequest request) {
        Integer userId = getUserId(request);
        return Result.ok(200, "获取成功", chatSessionService.getSessionList(userId));
    }

    @RequestMapping("/details")
    public Result<ChatSession> getDetails(@RequestParam Integer sessionId){
        ChatSession cs = chatSessionService.getSessionDetail(sessionId);
        if (cs == null) {
            return Result.fail(400, "会话不存在");
        }
        return Result.ok(200, null, cs);
    }

    @RequestMapping("/update")
    public Result<String> updateSession(@RequestParam Integer sessionId, @RequestParam String title) {
        chatSessionService.updateSession(sessionId, title);
        return Result.ok(200, "更新成功", null);
    }

    @RequestMapping("/delete")
    public Result<String> deleteSession(@RequestParam Integer sessionId) {
        ChatSession cs = chatSessionService.getSessionDetail(sessionId);
        if (cs == null) {
            return Result.fail(400, "会话不存在");
        }
        chatSessionService.deleteSession(sessionId);
        return Result.ok(200, "删除成功", null);
    }
}
