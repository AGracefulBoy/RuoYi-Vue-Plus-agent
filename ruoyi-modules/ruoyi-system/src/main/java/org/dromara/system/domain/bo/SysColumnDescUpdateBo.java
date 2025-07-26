package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 字段描述更新业务对象
 * 用于限制智能体只能更新字段描述字段
 *
 * @author ruoyi
 */
@Data
public class SysColumnDescUpdateBo {

    /**
     * 字段元数据ID
     */
    @NotNull(message = "字段元数据ID不能为空")
    private Long columnMetaId;

    /**
     * 字段名称（用于标识，不可修改）
     */
    @NotBlank(message = "字段名称不能为空")
    @Size(min = 1, max = 100, message = "字段名称长度必须在{min}到{max}个字符之间")
    private String columnName;

    /**
     * 字段描述
     */
    @Size(max = 1000, message = "字段描述长度不能超过{max}个字符")
    private String columnDesc;

} 