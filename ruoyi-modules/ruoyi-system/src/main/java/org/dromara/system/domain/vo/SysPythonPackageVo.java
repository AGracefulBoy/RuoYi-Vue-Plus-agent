package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysPythonPackage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Python包管理视图对象 sys_python_package
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysPythonPackage.class)
public class SysPythonPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 包ID
     */
    private Long packageId;

    /**
     * 租户编号
     */
    private String tenantId;

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
    private String delFlag;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}
