package com.zc.startai.dto;

import java.util.List;

/**
 * 活动规划的结构化结果
 * 模型的输出会被自动反序列化成这个对象，字段名即 JSON key，
 * description 会作为格式要求一并发给模型，指导它按字段含义填写
 */
public record ActivityPlan(
        String summary,
        List<Activity> activities,
        String bestTime,
        String risk,
        double confidence
) {

    public record Activity(
            String name,
            String reason,
            String durationMinutes
    ) {
    }
}
