package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token使用量DTO
 * 
 * @author system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageDto {
    
    /**
     * 提示词Token数量
     */
    private Integer promptTokens;
    
    /**
     * 生成Token数量
     */
    private Integer completionTokens;
    
    /**
     * 总Token数量
     */
    private Integer totalTokens;
    
    /**
     * 从IChatResponse.Usage转换
     */
    public static TokenUsageDto fromUsage(org.dromara.common.llm.model.protocol.resp.IChatResponse.Usage usage) {
        if (usage == null) {
            return TokenUsageDto.builder()
                .promptTokens(0)
                .completionTokens(0)
                .totalTokens(0)
                .build();
        }
        
        return TokenUsageDto.builder()
            .promptTokens(usage.getPromptTokens())
            .completionTokens(usage.getCompletionTokens())
            .totalTokens(usage.getTotalTokens())
            .build();
    }
}