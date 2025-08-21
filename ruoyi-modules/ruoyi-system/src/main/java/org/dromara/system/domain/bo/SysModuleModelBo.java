package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysModuleModel;

/**
 * 模块模型关联业务对象
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysModuleModel.class, reverseConvertGenerate = false)
public class SysModuleModelBo extends BaseEntity {

    /**
     * 主键
     */
    private Long id;

    /**
     * 模块ID
     */
    @NotNull(message = "模块ID不能为空", groups = {AddGroup.class})
    private Long moduleId;

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空", groups = {AddGroup.class})
    private Long modelId;

    /**
     * 是否默认模型（0否 1是）
     */
    private Integer isDefault;

    /**
     * 备注
     */
    private String remark;
}