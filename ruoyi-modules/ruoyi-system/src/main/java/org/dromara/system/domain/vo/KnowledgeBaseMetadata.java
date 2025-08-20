package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库元数据结构定义
 * 用于描述知识库的元数据字段
 *
 * @author ruoyi
 */
@Data
public class KnowledgeBaseMetadata implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段描述
     */
    private String desc;

    /**
     * 默认值
     */
    private String defaultValue;
}