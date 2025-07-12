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
import org.dromara.system.domain.SysPythonPackage;

/**
 * Python包管理业务对象 sys_python_package
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysPythonPackage.class, reverseConvertGenerate = false)
public class SysPythonPackageBo extends BaseEntity {

    /**
     * 包ID
     */
    @NotNull(message = "包ID不能为空", groups = { EditGroup.class })
    private Long packageId;

    /**
     * Python包名
     */
    @NotBlank(message = "Python包名不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "Python包名长度不能超过{max}个字符")
    private String packageName;

    /**
     * 包版本号
     */
    @NotBlank(message = "包版本号不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 50, message = "包版本号长度不能超过{max}个字符")
    private String packageVersion;

    /**
     * 包描述
     */
    @Size(min = 0, max = 500, message = "包描述长度不能超过{max}个字符")
    private String packageDescription;

    /**
     * 是否已安装（0未安装 1已安装）
     */
    private String isInstalled;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

} 