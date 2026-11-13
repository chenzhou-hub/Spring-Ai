package com.zc.startai.controller;

import com.zc.startai.dto.ActivityPlan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 个人助手接口
 * GET /assistant/chat?message=你好
 * GET /assistant/stream?message=今天适合跑步吗
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final ChatClient chatClient;
    private final ChatClient kbChatClient;

    public AssistantController(ChatClient assistantChatClient, ChatClient kbChatClient) {
        this.chatClient = assistantChatClient;
        this.kbChatClient = kbChatClient;
    }

    /** 同步对话 */
    @GetMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(defaultValue = "default") String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    /** 流式对话（SSE），浏览器或 Postman 可直接看到逐字输出 */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam String message,
                                                @RequestParam(defaultValue = "default") String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(token -> ServerSentEvent.builder(token).build());
    }

    /**
     * 结构化输出演示：返回的不是一段话，而是可直接使用的 Java 对象（自动序列化为 JSON）
     * Agent 会先调用工具查时间/天气，再综合生成结构化的活动规划
     */
    @GetMapping("/plan")
    public ActivityPlan plan(@RequestParam String message,
                             @RequestParam(defaultValue = "default") String conversationId) {
        return chatClient.prompt()
                .user(u -> u.text("""
                                根据我的需求制定一份活动规划：{message}
                                请先调用工具查询当前时间和天气，规划必须基于真实的工具查询结果。
                                """)
                        .param("message", message))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(ActivityPlan.class);
    }

    /**
     * RAG 知识库问答：QuestionAnswerAdvisor 自动完成
     * 问题向量化 → 检索最相关的文档片段 → 拼进 prompt 作为参考资料
     */
    @GetMapping("/kb")
    public String kb(@RequestParam String question) {
        return kbChatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
