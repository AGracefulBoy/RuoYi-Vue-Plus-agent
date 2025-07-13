package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysKnowledgeBaseConfig;

/**
 * 知识库默认配置业务对象 sys_knowledge_base_config
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysKnowledgeBaseConfig.class, reverseConvertGenerate = false)
public class SysKnowledgeBaseConfigBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long knowledgeBaseConfigId;

    /**
     * 向量检索返回条数
     */
    private Integer topK;

    /**
     * 向量检索权重
     */
    private Float vectorWeight;

    /**
     * 元数据
     */
    private String metadata;

    /**
     * 模型
     */
    private String model;

    /**
     * 块大小
     */
    private Integer blockSize;

    /**
     * 重叠字数
     */
    private Integer overlapSize;

    /**
     * 切片提示词
     */
    private String slicePrompt;

    /**
     * 图片识别提示词
     */
    private String imagePrompt;

    /**
     * 备注
     */
    private String remark;

} 