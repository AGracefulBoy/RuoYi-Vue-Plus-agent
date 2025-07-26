package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 数据源管理对象 sys_datasource
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_datasource")
public class SysDatasource extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    @TableId(value = "datasource_id")
    private Long datasourceId;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 数据源类型（database数据库、excel表格文件）
     */
    private String datasourceType;

    /**
     * 数据库类型（mysql、clickhouse、postgresql、oracle、sqlserver、sqlite等）
     */
    private String databaseType;

    /**
     * 数据库主机地址
     */
    private String host;

    /**
     * 数据库端口号
     */
    private Integer port;

    /**
     * 数据库名称/Schema
     */
    private String databaseName;

    /**
     * 数据库用户名
     */
    private String username;

    /**
     * 数据库密码（加密存储）
     */
    private String password;

    /**
     * 完整连接URL
     */
    private String connectionUrl;

    /**
     * 数据库驱动类名
     */
    private String driverClassName;

    /**
     * 文件路径（Excel等文件类型使用）
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * Excel工作表名称（JSON数组格式）
     */
    private String sheetNames;

    /**
     * 连接参数（JSON格式，如SSL配置、编码等）
     */
    private String connectionParams;

    /**
     * 最大连接数
     */
    private Integer maxConnections;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout;

    /**
     * 查询超时时间（毫秒）
     */
    private Integer queryTimeout;

    /**
     * 测试连接SQL
     */
    private String testQuery;

    /**
     * 数据源描述
     */
    private String description;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 是否默认数据源（0否 1是）
     */
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
     * 数据库同步状态（0 同步中 1 同步成功 2 同步失败）
     */
    private String syncStatus;

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