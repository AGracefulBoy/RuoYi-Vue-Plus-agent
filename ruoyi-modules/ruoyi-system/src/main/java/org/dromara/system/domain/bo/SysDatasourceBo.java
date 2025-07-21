package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysDatasource;

import java.time.LocalDateTime;

/**
 * 数据源管理业务对象 sys_datasource
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysDatasource.class)
public class SysDatasourceBo extends BaseEntity {

    /**
     * 数据源ID
     */
    @NotNull(message = "数据源ID不能为空", groups = { EditGroup.class })
    private Long datasourceId;

    /**
     * 数据源名称
     */
    @NotBlank(message = "数据源名称不能为空")
    @Size(min = 1, max = 100, message = "数据源名称长度必须在{min}到{max}个字符之间")
    private String datasourceName;

    /**
     * 数据源类型（database数据库、excel表格文件）
     */
    @NotBlank(message = "数据源类型不能为空")
    @Pattern(regexp = "^(database|excel)$", message = "数据源类型只能是database或excel")
    private String datasourceType;

    /**
     * 数据库类型（mysql、clickhouse、postgresql、oracle、sqlserver、sqlite等）
     */
    @Size(max = 50, message = "数据库类型长度不能超过{max}个字符")
    private String databaseType;

    /**
     * 数据库主机地址
     */
    @Size(max = 255, message = "主机地址长度不能超过{max}个字符")
    private String host;

    /**
     * 数据库端口号
     */
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号不能超过65535")
    private Integer port;

    /**
     * 数据库名称/Schema
     */
    @Size(max = 100, message = "数据库名称长度不能超过{max}个字符")
    private String databaseName;

    /**
     * 数据库用户名
     */
    @Size(max = 100, message = "用户名长度不能超过{max}个字符")
    private String username;

    /**
     * 数据库密码（加密存储）
     */
    @Size(max = 255, message = "密码长度不能超过{max}个字符")
    private String password;

    /**
     * 完整连接URL
     */
    @Size(max = 1000, message = "连接URL长度不能超过{max}个字符")
    private String connectionUrl;

    /**
     * 数据库驱动类名
     */
    @Size(max = 255, message = "驱动类名长度不能超过{max}个字符")
    private String driverClassName;

    /**
     * 文件路径（Excel等文件类型使用）
     */
    @Size(max = 500, message = "文件路径长度不能超过{max}个字符")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Min(value = 0, message = "文件大小不能为负数")
    private Long fileSize;

    /**
     * Excel工作表名称（JSON数组格式）
     */
    @Size(max = 1000, message = "工作表名称长度不能超过{max}个字符")
    private String sheetNames;

    /**
     * 连接参数（JSON格式，如SSL配置、编码等）
     */
    private String connectionParams;

    /**
     * 最大连接数
     */
    @Min(value = 1, message = "最大连接数必须大于0")
    @Max(value = 100, message = "最大连接数不能超过100")
    private Integer maxConnections;

    /**
     * 连接超时时间（毫秒）
     */
    @Min(value = 1000, message = "连接超时时间不能少于1000毫秒")
    @Max(value = 300000, message = "连接超时时间不能超过300000毫秒")
    private Integer connectionTimeout;

    /**
     * 查询超时时间（毫秒）
     */
    @Min(value = 1000, message = "查询超时时间不能少于1000毫秒")
    @Max(value = 600000, message = "查询超时时间不能超过600000毫秒")
    private Integer queryTimeout;

    /**
     * 测试连接SQL
     */
    @Size(max = 200, message = "测试SQL长度不能超过{max}个字符")
    private String testQuery;

    /**
     * 数据源描述
     */
    @Size(max = 500, message = "描述长度不能超过{max}个字符")
    private String description;

    /**
     * 状态（0正常 1停用）
     */
    @Pattern(regexp = "^[01]$", message = "状态只能是0或1")
    private String status;

    /**
     * 是否默认数据源（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否默认只能是0或1")
    private String isDefault;

    /**
     * 连接状态（0未测试 1连接成功 2连接失败）
     */
    private String connectionStatus;

    /**
     * 最后测试连接时间
     */
    private LocalDateTime lastTestTime;

    /**
     * 连接错误信息
     */
    private String errorMessage;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

} 