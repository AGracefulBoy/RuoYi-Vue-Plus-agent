package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 知识库文档分块视图对象 sys_knowledge_base_document_chunk
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysKnowledgeBaseDocumentChunk.class)
public class SysKnowledgeBaseDocumentChunkVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分块ID
     */
    @ExcelProperty(value = "分块ID")
    private Long chunkId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 所属文档ID
     */
    @ExcelProperty(value = "所属文档ID")
    private Long documentId;

    /**
     * 所属知识库ID
     */
    @ExcelProperty(value = "所属知识库ID")
    private Long knowledgeBaseId;

    /**
     * 分块序号
     */
    @ExcelProperty(value = "分块序号")
    private Integer chunkIndex;

    /**
     * 文档分块内容
     */
    @ExcelProperty(value = "文档分块内容")
    private String content;

    /**
     * 知识库文档名称
     */
    @ExcelProperty(value = "知识库文档名称")
    private String fileName;

    /**
     * 向量化状态（0待处理 1处理中 2已完成 3失败）
     */
    @ExcelProperty(value = "向量化状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_vector_status")
    private String vectorStatus;

    /**
     * 文档块元数据
     */
    private Map<String, Object> metadata;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}