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
 * 表元数据管理对象 sys_table_metadata
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_datasource_table_metadata")
public class SysDatasourceTableMetadata extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表元数据ID
     */
    @TableId(value = "table_meta_id")
    private Long tableMetaId;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 数据库名称/Schema名称
     */
    private String databaseName;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表注释
     */
    private String tableDesc;

    /**
     * 表类型（TABLE基表、VIEW视图、SYSTEM系统表等）
     */
    private String tableType;

    /**
     * 存储引擎（InnoDB、MyISAM、ClickHouse等）
     */
    private String engine;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    /**
     * 表行数（估算值）
     */
    private Long tableRows;

    /**
     * 数据大小（字节）
     */
    private Long dataLength;

    /**
     * 索引大小（字节）
     */
    private Long indexLength;

    /**
     * 表在数据库中的创建时间
     */
    private LocalDateTime createTimeDb;

    /**
     * 表在数据库中的更新时间
     */
    private LocalDateTime updateTimeDb;

    /**
     * 同步状态（0待同步 1同步成功 2同步失败）
     */
    private String syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 同步错误信息
     */
    private String syncErrorMessage;

    /**
     * 字段数量
     */
    private Integer fieldCount;

    /**
     * 索引数量
     */
    private Integer indexCount;

    /**
     * 是否分区表（0否 1是）
     */
    private String isPartitioned;

    /**
     * 分区信息（JSON格式）
     */
    private String partitionInfo;

    /**
     * 表结构DDL语句
     */
    private String tableStructure;

    /**
     * 业务描述
     */
    private String businessDescription;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

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
