package com.zc.startai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气工具：演示带参数的工具调用（数据为模拟数据）
 */
@Component
public class WeatherTools {

    private static final Map<String, String> FAKE_WEATHER = Map.of(
            "北京", "晴，8~18℃，微风，空气质量优，适合户外运动",
            "上海", "多云，15~22℃，湿度较高",
            "广州", "小雨，20~26℃，出门建议带伞",
            "深圳", "阴，19~25℃",
            "杭州", "晴转多云，12~20℃"
    );

    @Tool(description = "查询指定城市的当前天气情况（仅支持有限城市，数据为模拟数据）")
    public String getWeather(@ToolParam(description = "城市名称，例如：北京") String city) {
        return FAKE_WEATHER.getOrDefault(city,
                "[NO_DATA] 数据库中没有城市 " + city + " 的天气数据，请如实告知用户暂不支持该城市，不要编造");
    }
}
