package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据源简要信息视图对象
 * 用于智能体详情中的数据源列表展示
 *
 * @author 系统管理员
 */
@Data
public class SysDatasourceSimpleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
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
     * 数据库名称/Schema
     */
    private String databaseName;

    /**
     * 状态（0正常 1停用）
     */
    private String status;
}