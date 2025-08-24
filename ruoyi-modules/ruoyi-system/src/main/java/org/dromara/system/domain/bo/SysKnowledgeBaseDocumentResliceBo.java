package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.system.domain.SysKnowledgeBaseDocument;

import java.util.Map;

/**
 * 知识库文档重新切片业务对象
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysKnowledgeBaseDocument.class, reverseConvertGenerate = false)
public class SysKnowledgeBaseDocumentResliceBo {

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    /**
     * 文档元数据
     */
    private Map<String, Object> metadata;

    /**
     * 模型
     */
    private Long model;

    /**
     * 视觉模型
     */
    private Long imageModel;

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
     * 是否开启图片识别
     */
    private Integer enableImageRecognition;

    /**
     * 文档提取模式，1为AI提取，0为代码提取
     */
    private Integer mode;
}