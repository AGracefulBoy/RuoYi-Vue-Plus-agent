package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * Python包管理对象 sys_python_package
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_python_package")
public class SysPythonPackage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 包ID
     */
    @TableId(value = "package_id")
    private Long packageId;

    /**
     * Python包名
     */
    private String packageName;

    /**
     * 包版本号
     */
    private String packageVersion;

    /**
     * 包描述
     */
    private String packageDescription;

    /**
     * 是否已安装（0未安装 1已安装）
     */
    private String isInstalled;

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