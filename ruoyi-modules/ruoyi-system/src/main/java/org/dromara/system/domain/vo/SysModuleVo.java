package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysModule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 系统模块视图对象
 *
 * @author 系统管理员
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysModule.class)
public class SysModuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模块ID
     */
    @ExcelProperty(value = "模块ID")
    private Long moduleId;

    /**
     * 模块编码
     */
    @ExcelProperty(value = "模块编码")
    private String moduleCode;

    /**
     * 模块名称
     */
    @ExcelProperty(value = "模块名称")
    private String moduleName;

    /**
     * 模块描述
     */
    @ExcelProperty(value = "模块描述")
    private String moduleDesc;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "显示顺序")
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 关联的模型列表
     */
    private List<SysModelConfigVo> models;
}