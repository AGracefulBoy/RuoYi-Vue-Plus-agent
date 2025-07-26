package org.dromara.system.domain.bo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.system.domain.SysKnowledgeBaseDocument;

import java.util.Map;

/**
 * 知识库文档切片参数更新业务对象
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysKnowledgeBaseDocument.class, reverseConvertGenerate = false)
public class SysKnowledgeBaseDocumentSliceUpdateBo {

    /**
     * 文档ID
     */
    @NotNull(message = "文档ID不能为空", groups = { EditGroup.class })
    private Long documentId;

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
}
