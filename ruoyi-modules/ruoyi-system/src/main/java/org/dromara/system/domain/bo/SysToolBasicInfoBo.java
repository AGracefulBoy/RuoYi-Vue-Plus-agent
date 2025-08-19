package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysTool;

/**
 * 工具基本信息更新业务对象 sys_tool
 * 用于仅更新工具的基本信息，不影响技术配置
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTool.class, reverseConvertGenerate = false)
public class SysToolBasicInfoBo extends BaseEntity {

    /**
     * 工具ID
     */
    @NotNull(message = "工具ID不能为空", groups = { EditGroup.class })
    private Long toolId;

    /**
     * 工具名称
     */
    @Size(min = 0, max = 100, message = "工具名称长度不能超过{max}个字符")
    private String toolName;

    /**
     * 工具描述
     */
    @Size(min = 0, max = 500, message = "工具描述长度不能超过{max}个字符")
    private String toolDesc;

    /**
     * 工具状态（0正常 1停用）
     */
    private String toolStatus;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

    /**
     * 执行超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 速率限制（次/分钟，0表示不限制）
     */
    private Integer rateLimit;
}