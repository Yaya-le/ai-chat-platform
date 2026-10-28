package com.zykj.ai_backend.ai;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class AIEngineClient {

    private final WebClient webClient = WebClient.builder().build();

    // 调用AIEngine服务
    public String chatComplete(String message,String model,List<Map<String,String>> history,String ststem){

        return webClient.post()
                .uri("http://127.0.0.1:8001/chat/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", message,"model","default", "history", history, "system", ststem))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}