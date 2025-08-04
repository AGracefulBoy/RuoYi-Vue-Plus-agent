package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysKnowledgeBaseDocument;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 知识库文档管理视图对象 sys_knowledge_base_document
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysKnowledgeBaseDocument.class)
public class SysKnowledgeBaseDocumentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @ExcelProperty(value = "文档ID")
    private Long documentId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 所属知识库ID
     */
    @ExcelProperty(value = "所属知识库ID")
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    @ExcelProperty(value = "知识库名称")
    private String knowledgeBaseName;

    /**
     * 文档名称
     */
    @ExcelProperty(value = "文档名称")
    private String name;

    /**
     * 上传时间
     */
    @ExcelProperty(value = "上传时间")
    private Date uploadTime;

    /**
     * 文档URL
     */
    @ExcelProperty(value = "文档URL")
    private String url;

    /**
     * 文档类型
     */
    @ExcelProperty(value = "文档类型")
    private String type;

    /**
     * 文档大小(KB)
     */
    @ExcelProperty(value = "文档大小(KB)")
    private Long size;

    /**
     * 处理状态（to_be_executed 待执行, document_parsing 解析中, document_embedding 文档向量化, document_finished 已完成, document_fail 失败）
     */
    @ExcelProperty(value = "处理状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_document_status")
    private String status;

    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;

    /**
     * 模型
     */
    @ExcelProperty(value = "模型")
    private Long model;

    /**
     * 块大小
     */
    @ExcelProperty(value = "块大小")
    private Integer blockSize;

    /**
     * 重叠字数
     */
    @ExcelProperty(value = "重叠字数")
    private Integer overlapSize;

    /**
     * 切片提示词
     */
    @ExcelProperty(value = "切片提示词")
    private String slicePrompt;

    /**
     * 图片识别提示词
     */
    @ExcelProperty(value = "图片识别提示词")
    private String imagePrompt;

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

    /**
     * 任务id
     */
    @ExcelProperty(value = "任务id")
    private String taskId;

    /**
     * 解析完成后的URL
     */
    @ExcelProperty(value = "解析完成后的URL")
    private String parseCompletedUrl;

    /**
     * 是否开启图片识别
     */
    @ExcelProperty(value = "是否开启图片识别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private Integer enableImageRecognition;

    /**
     * 文档提取模式，1为AI提取，0为代码提取
     */
    @ExcelProperty(value = "文档提取模式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_document_extract_mode")
    private Integer mode;

}
