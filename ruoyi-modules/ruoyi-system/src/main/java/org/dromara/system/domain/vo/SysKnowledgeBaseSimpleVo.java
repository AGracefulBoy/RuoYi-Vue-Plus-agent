package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库简要信息视图对象
 * 用于智能体详情中的知识库列表展示
 *
 * @author 系统管理员
 */
@Data
public class SysKnowledgeBaseSimpleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 模型
     */
    private Long model;

    /**
     * 状态（0正常 1停用）
     */
    private String status;
}
