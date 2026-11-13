package com.zc.startai.config;

import com.zc.startai.tools.CalculatorTools;
import com.zc.startai.tools.DateTimeTools;
import com.zc.startai.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 个人助手配置：组装 ChatClient（模型 + 记忆 + 工具）
 */
@Configuration
public class AssistantConfig {

    private static final String SYSTEM_PROMPT = """
            你是"小助"，一个友好的个人助手。
            你可以调用工具来查询时间、天气和做数学计算。
            回答时注意：
            1. 涉及当前时间、日期的问题，必须调用时间工具获取，不要凭空猜测；
            2. 被问到天气时，调用天气工具查询，并基于天气数据给出建议（例如是否适合运动）；
            3. 数学运算必须调用计算器工具，不要心算；
            4. 一次提问可能需要连续调用多个工具，请按需调用后再综合回答；
            5. 严禁编造事实：如果工具返回"暂无数据"或调用失败，必须如实告知用户没有该数据，绝不能根据你自己的知识杜撰天气、时间等信息；
            6. 天气等时效性信息只能以工具返回结果为准，你的训练知识里的天气信息一律不可信；
            7. 用简洁的中文回答。
            """;

    @Bean
    public ChatMemory chatMemory() {
        // 窗口记忆：最多保留最近 20 条消息（含工具调用消息）
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient assistantChatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                                          ToolCallbackProvider mcpToolCallbackProvider,
                                          DateTimeTools dateTimeTools,
                                          WeatherTools weatherTools,
                                          CalculatorTools calculatorTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                // 本地 @Tool 工具 + MCP 工具（来自远端 Server）：2.0 起统一由 defaultTools 注册
                .defaultTools(dateTimeTools, weatherTools, calculatorTools, mcpToolCallbackProvider)
                // 挂载记忆 Advisor：自动把历史对话带入上下文
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
