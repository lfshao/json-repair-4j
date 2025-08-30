package io.github.lfshao.json.repair;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON修复工具的主入口类
 * 提供修复格式不正确的JSON字符串的功能
 */
public class JsonRepair {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 修复格式不正确的JSON字符串
     *
     * @param jsonStr 需要修复的JSON字符串
     * @return 修复后的有效JSON字符串
     */
    public static String repair(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return "";
        }

        JsonParser parser = new JsonParser(jsonStr, false, false);

        // 首先尝试使用标准JSON解析器
        try {
            Object parsed = objectMapper.readValue(jsonStr, Object.class);
            String result = objectMapper.writeValueAsString(parsed);

            // 检查是否Jackson只解析了部分内容（多JSON情况）
            // 如果结果比输入短，可能存在多JSON
            if (result.length() < jsonStr.trim().length()) {
                // 可能存在多JSON，使用修复解析器
                throw new JsonProcessingException("Possible multiple JSON elements") {
                };
            }

            return result;
        } catch (JsonProcessingException e) {
            // 标准解析失败，使用修复解析器
            Object parsed = parser.parse();

            if (parsed == null || "".equals(parsed)) {
                return "";
            }

            try {
                return objectMapper.writeValueAsString(parsed);
            } catch (JsonProcessingException ex) {
                // 如果还是无法序列化，返回原字符串
                return jsonStr;
            }
        }
    }
} 