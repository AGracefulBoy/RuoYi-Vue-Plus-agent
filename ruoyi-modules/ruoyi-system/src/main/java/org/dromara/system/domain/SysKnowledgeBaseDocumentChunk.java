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
import java.util.Map;

/**
 * 知识库文档分块对象 sys_knowledge_base_document_chunk
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_knowledge_base_document_chunk", autoResultMap = true)
public class SysKnowledgeBaseDocumentChunk extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分块ID
     */
    @TableId(value = "chunk_id")
    private Long chunkId;

    /**
     * 所属文档ID
     */
    private Long documentId;

    /**
     * 所属知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 文档分块内容
     */
    private String content;

    /**
     * 知识库文档名称
     */
    private String fileName;

    /**
     * 向量化状态（0待处理 1处理中 2已完成 3失败）
     */
    private String vectorStatus;

    /**
     * 文档块元数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}