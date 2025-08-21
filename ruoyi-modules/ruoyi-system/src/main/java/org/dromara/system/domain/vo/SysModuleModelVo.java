package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysModuleModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 模块模型关联视图对象
 *
 * @author 系统管理员
 */
@Data
@AutoMapper(target = SysModuleModel.class)
public class SysModuleModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
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
     * 租户编号
     */
    private String tenantId;

    /**
     * 备注
     */
    private String remark;

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
     * 模块信息
     */
    private SysModuleVo module;

    /**
     * 模型信息
     */
    private SysModelConfigVo model;
}