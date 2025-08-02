package org.dromara.system.service.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具参数验证器
 * 基于JSON Schema规范验证参数
 */
@Slf4j
@Component
public class ToolParameterValidator {
    
    /**
     * 验证参数是否符合Schema定义
     *
     * @param parameters 待验证的参数（JSON格式）
     * @param schemaJson JSON Schema定义
     * @return 验证结果
     */
    public ValidationResult validate(String parameters, String schemaJson) {
        if (StrUtil.isBlank(schemaJson)) {
            // 如果没有定义schema，认为任何参数都是有效的
            return ValidationResult.success();
        }
        
        try {
            JSONObject schema = JSONUtil.parseObj(schemaJson);
            JSONObject params = StrUtil.isBlank(parameters) ? new JSONObject() : JSONUtil.parseObj(parameters);
            
            List<String> errors = new ArrayList<>();
            validateObject(params, schema, "", errors);
            
            if (errors.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure(errors);
            }
        } catch (Exception e) {
            log.error("参数验证失败", e);
            return ValidationResult.failure("参数验证异常: " + e.getMessage());
        }
    }
    
    /**
     * 验证对象类型的参数
     */
    private void validateObject(JSONObject data, JSONObject schema, String path, List<String> errors) {
        String type = schema.getStr("type");
        
        // 验证类型
        if ("object".equals(type)) {
            JSONObject properties = schema.getJSONObject("properties");
            if (properties != null) {
                // 验证每个属性
                for (String key : properties.keySet()) {
                    JSONObject propSchema = properties.getJSONObject(key);
                    Object value = data.get(key);
                    String propPath = StrUtil.isBlank(path) ? key : path + "." + key;
                    
                    validateProperty(value, propSchema, propPath, errors);
                }
            }
            
            // 验证必填字段
            JSONArray required = schema.getJSONArray("required");
            if (required != null) {
                for (Object reqField : required) {
                    String fieldName = reqField.toString();
                    if (!data.containsKey(fieldName)) {
                        errors.add("缺少必填字段: " + (StrUtil.isBlank(path) ? fieldName : path + "." + fieldName));
                    }
                }
            }
        }
    }
    
    /**
     * 验证单个属性
     */
    private void validateProperty(Object value, JSONObject schema, String path, List<String> errors) {
        String type = schema.getStr("type");
        
        if (value == null) {
            // 如果值为null，检查是否在required中
            return;
        }
        
        // 类型验证
        switch (type) {
            case "string":
                if (!(value instanceof String)) {
                    errors.add(path + " 应该是字符串类型");
                }
                break;
            case "integer":
                if (!(value instanceof Number)) {
                    errors.add(path + " 应该是整数类型");
                } else {
                    validateNumberConstraints(((Number) value).intValue(), schema, path, errors);
                }
                break;
            case "number":
                if (!(value instanceof Number)) {
                    errors.add(path + " 应该是数字类型");
                } else {
                    validateNumberConstraints(((Number) value).doubleValue(), schema, path, errors);
                }
                break;
            case "boolean":
                if (!(value instanceof Boolean)) {
                    errors.add(path + " 应该是布尔类型");
                }
                break;
            case "array":
                if (!(value instanceof JSONArray)) {
                    errors.add(path + " 应该是数组类型");
                }
                break;
            case "object":
                if (value instanceof JSONObject) {
                    validateObject((JSONObject) value, schema, path, errors);
                } else {
                    errors.add(path + " 应该是对象类型");
                }
                break;
        }
    }
    
    /**
     * 验证数字约束
     */
    private void validateNumberConstraints(Number value, JSONObject schema, String path, List<String> errors) {
        if (schema.containsKey("minimum")) {
            double min = schema.getDouble("minimum");
            if (value.doubleValue() < min) {
                errors.add(path + " 不能小于 " + min);
            }
        }
        
        if (schema.containsKey("maximum")) {
            double max = schema.getDouble("maximum");
            if (value.doubleValue() > max) {
                errors.add(path + " 不能大于 " + max);
            }
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public static ValidationResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
}