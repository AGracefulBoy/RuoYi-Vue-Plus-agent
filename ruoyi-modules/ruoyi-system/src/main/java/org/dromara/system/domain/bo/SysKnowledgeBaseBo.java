package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.vo.KnowledgeBaseMetadata;

import java.util.List;

/**
 * 知识库管理业务对象 sys_knowledge_base
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysKnowledgeBase.class, reverseConvertGenerate = false)
public class SysKnowledgeBaseBo extends BaseEntity {

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空", groups = { EditGroup.class })
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 255, message = "知识库名称长度不能超过{max}个字符")
    private String name;

    /**
     * 知识库描述
     */
    @Size(min = 0, max = 1000, message = "知识库描述长度不能超过{max}个字符")
    private String description;

    /**
     * 向量检索返回条数
     */
    @NotNull(message = "向量检索返回条数不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer topK;

    /**
     * 向量检索权重
     */
    @NotNull(message = "向量检索权重不能为空", groups = { AddGroup.class, EditGroup.class })
    @DecimalMin(value = "0.0", message = "向量检索权重不能小于0")
    @DecimalMax(value = "1.0", message = "向量检索权重不能大于1")
    private Float vectorWeight;

    /**
     * 元数据
     */
    @Size(min = 0, max = 2000, message = "元数据长度不能超过{max}个字符")
    private String metadata;

    /**
     * 元数据列表（用于前端传入）
     */
    private List<KnowledgeBaseMetadata> metadataList;

    /**
     * 模型
     */
    private Long model;

    /**
     * 视觉模型
     */
    private Long imageModel;

    /**
     * 向量模型
     */
    private Long embeddingModel;

    /**
     * 重排序模型
     */
    private Long rerankModel;

    /**
     * 块大小
     */
    @NotNull(message = "块大小不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer blockSize;

    /**
     * 重叠字数
     */
    @NotNull(message = "重叠字数不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer overlapSize;

    /**
     * 切片提示词
     */
    @Size(min = 0, max = 2000, message = "切片提示词长度不能超过{max}个字符")
    private String slicePrompt;

    /**
     * 图片识别提示词
     */
    @Size(min = 0, max = 2000, message = "图片识别提示词长度不能超过{max}个字符")
    private String imagePrompt;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

}
