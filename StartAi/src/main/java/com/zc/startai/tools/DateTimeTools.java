package com.zc.startai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具：演示最简单的无参/有参工具调用
 */
@Component
public class DateTimeTools {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE");

    @Tool(description = "获取当前的日期和时间，例如 2026-11-13 14:30:00 星期五")
    public String currentTime() {
        return LocalDateTime.now().format(FORMATTER);
    }

    @Tool(description = "获取指定偏移小时数之后的时间，offsetHours 可以为负数表示过去")
    public String timeAfter(@ToolParam(description = "相对当前时间的偏移小时数") int offsetHours) {
        return LocalDateTime.now().plusHours(offsetHours).format(FORMATTER);
    }
}
