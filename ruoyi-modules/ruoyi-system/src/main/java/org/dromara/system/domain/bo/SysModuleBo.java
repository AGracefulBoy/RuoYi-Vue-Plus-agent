package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysModule;

/**
 * 系统模块业务对象
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysModule.class, reverseConvertGenerate = false)
public class SysModuleBo extends BaseEntity {

    /**
     * 模块ID
     */
    @NotNull(message = "模块ID不能为空", groups = {EditGroup.class})
    private Long moduleId;

    /**
     * 模块编码
     */
    @NotBlank(message = "模块编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String moduleCode;

    /**
     * 模块名称
     */
    @NotBlank(message = "模块名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String moduleName;

    /**
     * 模块描述
     */
    private String moduleDesc;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    private String status;
}