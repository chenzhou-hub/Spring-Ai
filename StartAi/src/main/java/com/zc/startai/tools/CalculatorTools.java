package com.zc.startai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具：让模型把数学运算交给工具执行，避免 LLM 自己算错
 */
@Component
public class CalculatorTools {

    @Tool(description = "计算两个数字的四则运算结果")
    public double calculate(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "运算符，仅支持 + - * /") String operator,
            @ToolParam(description = "第二个数字") double b) {
        return switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) {
                    throw new IllegalArgumentException("除数不能为 0");
                }
                yield a / b;
            }
            default -> throw new IllegalArgumentException("不支持的运算符: " + operator);
        };
    }
}
