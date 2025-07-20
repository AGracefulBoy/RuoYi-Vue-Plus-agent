package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysDatasource;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据源管理列表视图对象 sys_datasource
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysDatasource.class)
public class SysDatasourceListVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @ExcelProperty(value = "数据源ID")
    private Long datasourceId;

    /**
     * 数据源名称
     */
    @ExcelProperty(value = "数据源名称")
    private String datasourceName;

    /**
     * 数据源类型（database数据库、excel表格文件）
     */
    @ExcelProperty(value = "数据源类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_datasource_type")
    private String datasourceType;

    /**
     * 创建者名称
     */
    @ExcelProperty(value = "创建者")
    private String createByName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

}