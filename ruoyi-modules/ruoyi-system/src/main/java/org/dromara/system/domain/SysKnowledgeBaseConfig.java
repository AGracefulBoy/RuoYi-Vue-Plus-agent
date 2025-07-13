package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 知识库默认配置表 sys_knowledge_base_config
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_knowledge_base_config")
public class SysKnowledgeBaseConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "knowledge_base_config_id")
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
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

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