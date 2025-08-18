package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具包关联对象 sys_tool_package
 *
 * @author ruoyi
 */
@Data
@TableName("sys_tool_package")
public class SysToolPackage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 包ID（自增主键）
     */
    @TableId(value = "package_id", type = IdType.AUTO)
    private Long packageId;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 包版本号
     */
    private String packageVersion;

}
