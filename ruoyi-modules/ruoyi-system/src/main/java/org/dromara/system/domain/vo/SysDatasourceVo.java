package org.dromara.system.domain.vo;


import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveStrategy;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.system.domain.SysDatasource;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据源管理视图对象 sys_datasource
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDatasource.class)
public class SysDatasourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @ExcelProperty(value = "数据源ID")
    private Long datasourceId;

    /**
     * 租户编号
     */
    @ExcelProperty(value = "租户编号")
    private String tenantId;

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
     * 数据库类型（mysql、clickhouse、postgresql、oracle、sqlserver、sqlite等）
     */
    @ExcelProperty(value = "数据库类型")
    private String databaseType;

    /**
     * 数据库主机地址
     */
    @ExcelProperty(value = "主机地址")
    private String host;

    /**
     * 数据库端口号
     */
    @ExcelProperty(value = "端口号")
    private Integer port;

    /**
     * 数据库名称/Schema
     */
    @ExcelProperty(value = "数据库名称")
    private String databaseName;

    /**
     * 数据库用户名
     */
    @ExcelProperty(value = "用户名")
    private String username;

    /**
     * 数据库密码（加密存储）
     * 敏感信息脱敏处理
     */
    @Sensitive(strategy = SensitiveStrategy.PASSWORD)
    private String password;

    /**
     * 完整连接URL
     */
    @ExcelProperty(value = "连接URL")
    private String connectionUrl;

    /**
     * 数据库驱动类名
     */
    @ExcelProperty(value = "驱动类名")
    private String driverClassName;

    /**
     * 文件路径（Excel等文件类型使用）
     */
    @ExcelProperty(value = "文件路径")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @ExcelProperty(value = "文件大小")
    private Long fileSize;

    /**
     * Excel工作表名称（JSON数组格式）
     */
    @ExcelProperty(value = "工作表名称")
    private String sheetNames;

    /**
     * 连接参数（JSON格式，如SSL配置、编码等）
     */
    private String connectionParams;

    /**
     * 最大连接数
     */
    @ExcelProperty(value = "最大连接数")
    private Integer maxConnections;

    /**
     * 连接超时时间（毫秒）
     */
    @ExcelProperty(value = "连接超时时间")
    private Integer connectionTimeout;

    /**
     * 查询超时时间（毫秒）
     */
    @ExcelProperty(value = "查询超时时间")
    private Integer queryTimeout;

    /**
     * 测试连接SQL
     */
    @ExcelProperty(value = "测试SQL")
    private String testQuery;

    /**
     * 数据源描述
     */
    @ExcelProperty(value = "描述")
    private String description;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 是否默认数据源（0否 1是）
     */
    @ExcelProperty(value = "是否默认", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isDefault;

    /**
     * 连接状态（0未测试 1连接成功 2连接失败）
     */
    @ExcelProperty(value = "连接状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=未测试,1=连接成功,2=连接失败")
    private String connectionStatus;

    /**
     * 最后测试连接时间
     */
    @ExcelProperty(value = "最后测试时间")
    private LocalDateTime lastTestTime;

    /**
     * 连接错误信息
     */
    private String errorMessage;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建者ID
     */
    private Long createBy;

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
     * 更新者ID
     */
    private Long updateBy;

    /**
     * 更新者名称
     */
    @ExcelProperty(value = "更新者")
    private String updateByName;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private LocalDateTime updateTime;

}
