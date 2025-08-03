package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;
import java.util.Map;

/**
 * 知识库文档管理对象 sys_knowledge_base_document
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_knowledge_base_document", autoResultMap = true)
public class SysKnowledgeBaseDocument extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @TableId(value = "document_id")
    private Long documentId;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 上传时间
     */
    private Date uploadTime;

    /**
     * 文档URL
     */
    private String url;

    /**
     * 文档类型
     */
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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 模型
     */
    private Long model;

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
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * 任务id
     */
    private String taskId;

    /**
     * 解析完成后的url
     */
    private String parseCompletedUrl;

} 