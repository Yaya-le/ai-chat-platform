package com.zykj.ai_backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "欢迎访问 AI Backend 服务！";
    }

    @GetMapping("/hello")
    public String hello() {
        return "你好，请问需要什么帮助？";
    }

    @GetMapping("/api/hello")
    public String apiHello() {
        return "Hello World!";
    }
}
