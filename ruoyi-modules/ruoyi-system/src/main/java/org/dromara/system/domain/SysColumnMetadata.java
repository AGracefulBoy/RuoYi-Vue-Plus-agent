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
 * 字段元数据管理对象 sys_column_metadata
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_column_metadata")
public class SysColumnMetadata extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段元数据ID
     */
    @TableId(value = "column_meta_id")
    private Long columnMetaId;

    /**
     * 表元数据ID
     */
    private Long tableMetaId;

    /**
     * 数据源ID（冗余字段，便于查询）
     */
    private Long datasourceId;

    /**
     * 数据库名称（冗余字段，便于查询）
     */
    private String databaseName;

    /**
     * 表名称（冗余字段，便于查询）
     */
    private String tableName;

    /**
     * 字段名称
     */
    private String columnName;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 字段在表中的位置（从1开始）
     */
    private Integer ordinalPosition;

    /**
     * 字段默认值
     */
    private String columnDefault;

    /**
     * 是否允许NULL（YES、NO）
     */
    private String isNullable;

    /**
     * 数据类型（varchar、int、bigint等）
     */
    private String dataType;

    /**
     * 字符类型最大长度
     */
    private Integer characterMaximumLength;

    /**
     * 数值类型精度
     */
    private Integer numericPrecision;

    /**
     * 数值类型小数位数
     */
    private Integer numericScale;

    /**
     * 日期时间类型精度
     */
    private Integer datetimePrecision;

    /**
     * 字符集名称
     */
    private String characterSetName;

    /**
     * 排序规则名称
     */
    private String collationName;

    /**
     * 完整的字段类型（含长度，如varchar(255)）
     */
    private String columnType;

    /**
     * 键类型（PRI主键、UNI唯一键、MUL普通索引）
     */
    private String columnKey;

    /**
     * 额外信息（auto_increment、on update current_timestamp等）
     */
    private String extra;

    /**
     * 是否主键（0否 1是）
     */
    private String isPrimaryKey;

    /**
     * 是否外键（0否 1是）
     */
    private String isForeignKey;

    /**
     * 是否唯一键（0否 1是）
     */
    private String isUniqueKey;

    /**
     * 是否有索引（0否 1是）
     */
    private String isIndexed;

    /**
     * 外键关联的表名
     */
    private String foreignKeyTable;

    /**
     * 外键关联的字段名
     */
    private String foreignKeyColumn;

    /**
     * 业务字段名称
     */
    private String businessName;

    /**
     * 业务描述
     */
    private String businessDescription;

    /**
     * 数据分类（个人信息、敏感信息、业务信息等）
     */
    private String dataClassification;

    /**
     * 敏感级别（public公开、internal内部、confidential机密、secret绝密）
     */
    private String sensitivityLevel;

    /**
     * 是否个人身份信息（0否 1是）
     */
    private String isPii;

    /**
     * 脱敏规则
     */
    private String maskingRule;

    /**
     * 数据验证规则（正则表达式、范围等）
     */
    private String validationRule;

    /**
     * 示例值（JSON数组格式）
     */
    private String sampleValues;

    /**
     * 同步状态（0待同步 1同步成功 2同步失败）
     */
    private String syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

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