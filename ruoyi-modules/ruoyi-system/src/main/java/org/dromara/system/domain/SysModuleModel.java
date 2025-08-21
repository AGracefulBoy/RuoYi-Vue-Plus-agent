package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 模块模型关联表 sys_module_model
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_module_model")
public class SysModuleModel extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 模块ID
     */
    private Long moduleId;

    /**
     * 模型ID
     */
    private Long modelId;

    /**
     * 是否默认模型（0否 1是）
     */
    private Integer isDefault;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}