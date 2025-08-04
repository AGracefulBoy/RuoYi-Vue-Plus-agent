package org.dromara.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库搜索请求参数
 *
 * @author ruoyi
 */
@Data
public class KnowledgeSearchRequest {

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    /**
     * 搜索问题
     */
    @NotBlank(message = "搜索问题不能为空")
    private String question;

    /**
     * 元数据过滤条件（JSON字符串）
     */
    private String metadata;

    /**
     * 是否为知识库搜索（影响metadata处理方式）
     */
    private Boolean isKnowledge = true;
}
