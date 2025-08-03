package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysKnowledgeBaseDocument;

import java.util.Date;
import java.util.Map;

/**
 * 知识库文档管理业务对象 sys_knowledge_base_document
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysKnowledgeBaseDocument.class, reverseConvertGenerate = false)
public class SysKnowledgeBaseDocumentBo extends BaseEntity {

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空", groups = { EditGroup.class })
    private Long documentId;

    /**
     * 所属知识库ID
     */
    @NotNull(message = "所属知识库ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long knowledgeBaseId;

    /**
     * 文档名称
     */
    @NotBlank(message = "文档名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 255, message = "文档名称长度不能超过{max}个字符")
    private String name;

    /**
     * 上传时间
     */
    private Date uploadTime;

    /**
     * 文档URL
     */
    @Size(min = 0, max = 500, message = "文档URL长度不能超过{max}个字符")
    private String url;

    /**
     * 文档类型
     */
    @Size(min = 0, max = 50, message = "文档类型长度不能超过{max}个字符")
    private String type;

    /**
     * 文档大小(KB)
     */
    private Long size;

    /**
     * 处理状态（to_be_executed 待执行, document_parsing 解析中, document_embedding 文档向量化, document_finished 已完成, document_fail 失败）
     */
    private String status;

    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;

    /**
     * 模型
     */
    @Size(min = 0, max = 255, message = "模型长度不能超过{max}个字符")
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
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

    /**
     * 任务id
     */
    @Size(min = 0, max = 256, message = "任务id长度不能超过{max}个字符")
    private String taskId;

    /**
     * 解析完成后的URL
     */
    @Size(min = 0, max = 512, message = "解析完成后的URL长度不能超过{max}个字符")
    private String parseCompletedUrl;

} 