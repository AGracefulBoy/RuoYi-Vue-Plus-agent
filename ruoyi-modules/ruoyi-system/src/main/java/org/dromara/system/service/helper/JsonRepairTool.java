package org.dromara.system.service.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class JsonRepairTool {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 修复JSON字符串
     * @param jsonString 输入的JSON字符串
     * @return 修复后的JSON字符串
     */
    public static String repairJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return "{}";
        }

        try {
            // 1. 基本清理和预处理
            String cleanedJson = preprocessJson(jsonString);

            // 2. 尝试解析JSON
            JsonNode jsonNode = objectMapper.readTree(cleanedJson);

            // 3. 验证和修复数据结构
            JsonNode repairedNode = repairJsonStructure(jsonNode);

            // 4. 返回格式化的JSON字符串
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(repairedNode);

        } catch (JsonProcessingException e) {
            // 如果JSON解析失败，尝试更激进的修复
            return attemptAggressiveRepair(jsonString);
        }
    }

    /**
     * JSON预处理
     */
    private static String preprocessJson(String jsonString) {
        String processed = jsonString.trim();

        // 移除BOM标记
        if (processed.startsWith("\uFEFF")) {
            processed = processed.substring(1);
        }

        // 修复常见的引号问题
        processed = fixQuotes(processed);

        // 移除多余的逗号
        processed = removeTrailingCommas(processed);

        // 修复转义字符
        processed = fixEscapeCharacters(processed);

        return processed;
    }

    /**
     * 修复引号问题
     */
    private static String fixQuotes(String jsonString) {
        // 将中文引号替换为英文引号
        jsonString = jsonString.replaceAll("“", "\"").replaceAll("”", "\"");
        jsonString = jsonString.replaceAll("'", "\"");

        // 修复属性名缺少引号的问题
        Pattern pattern = Pattern.compile("(\\{|,)\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
        Matcher matcher = pattern.matcher(jsonString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + "\"" + matcher.group(2) + "\":");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 移除多余的逗号
     */
    private static String removeTrailingCommas(String jsonString) {
        // 移除对象和数组末尾的多余逗号
        jsonString = jsonString.replaceAll(",\\s*}", "}");
        jsonString = jsonString.replaceAll(",\\s*]", "]");
        return jsonString;
    }

    /**
     * 修复转义字符
     */
    private static String fixEscapeCharacters(String jsonString) {
        // 修复常见的转义字符问题
        // 将无效的转义序列（反斜杠后不是有效JSON转义字符的）转换为双反斜杠
        // 有效的JSON转义字符: \" \\ \/ \b \f \n \r \t 以及Unicode转义
        jsonString = jsonString.replaceAll("\\\\(?![\"\\\\/bfnrtu])", "\\\\\\\\");
        return jsonString;
    }

    /**
     * 修复JSON数据结构
     */
    private static JsonNode repairJsonStructure(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode repairedObject = objectMapper.createObjectNode();

            objectNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                // 递归修复子节点
                JsonNode repairedValue = repairJsonStructure(value);
                repairedObject.set(key, repairedValue);
            });

            return repairedObject;

        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode repairedArray = objectMapper.createArrayNode();

            for (JsonNode item : arrayNode) {
                // 递归修复数组元素
                JsonNode repairedItem = repairJsonStructure(item);
                repairedArray.add(repairedItem);
            }

            return repairedArray;
        }

        // 基本类型直接返回
        return node;
    }

    /**
     * 激进的JSON修复尝试
     */
    private static String attemptAggressiveRepair(String jsonString) {
        try {
            // 尝试添加缺失的大括号
            String repaired = jsonString.trim();
            if (!repaired.startsWith("{") && !repaired.startsWith("[")) {
                repaired = "{" + repaired + "}";
            }

            // 再次尝试解析
            JsonNode node = objectMapper.readTree(repaired);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);

        } catch (Exception e) {
            // 如果仍然失败，返回错误信息的JSON格式
            return "{\"error\":\"无法修复的JSON格式\",\"original\":\"" +
                jsonString.replaceAll("\"", "\\\\\"") + "\"}";
        }
    }
}
