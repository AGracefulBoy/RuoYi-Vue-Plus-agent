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
import org.dromara.system.domain.SysTool;

/**
 * 工具管理业务对象 sys_tool
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTool.class, reverseConvertGenerate = false)
public class SysToolBo extends BaseEntity {

    /**
     * 工具ID
     */
    @NotNull(message = "工具ID不能为空", groups = { EditGroup.class })
    private Long toolId;

    /**
     * 工具名称
     */
    @NotBlank(message = "工具名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "工具名称长度不能超过{max}个字符")
    private String toolName;

    /**
     * 工具描述
     */
    @Size(min = 0, max = 500, message = "工具描述长度不能超过{max}个字符")
    private String toolDesc;

    /**
     * 函数名称
     */
    @NotBlank(message = "函数名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 200, message = "函数名称长度不能超过{max}个字符")
    private String functionName;

    /**
     * 工具类型（api、script、builtin等）
     */
    @NotBlank(message = "工具类型不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 50, message = "工具类型长度不能超过{max}个字符")
    private String toolType;

    /**
     * 是否流式处理（0否 1是）
     */
    private String isStream;

    /**
     * 脚本代码
     */
    private String scriptCode;

    /**
     * 工具状态（0正常 1停用）
     */
    private String toolStatus;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

} 