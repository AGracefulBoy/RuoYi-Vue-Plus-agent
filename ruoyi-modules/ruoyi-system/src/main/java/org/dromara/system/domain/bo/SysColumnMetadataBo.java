package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysColumnMetadata;

import java.time.LocalDateTime;

/**
 * 字段元数据管理业务对象 sys_column_metadata
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysColumnMetadata.class, reverseConvertGenerate = false)
public class SysColumnMetadataBo extends BaseEntity {

    /**
     * 字段元数据ID
     */
    @NotNull(message = "字段元数据ID不能为空", groups = { EditGroup.class })
    private Long columnMetaId;

    /**
     * 表元数据ID
     */
    @NotNull(message = "表元数据ID不能为空")
    private Long tableMetaId;

    /**
     * 数据源ID（冗余字段，便于查询）
     */
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    /**
     * 数据库名称（冗余字段，便于查询）
     */
    @NotBlank(message = "数据库名称不能为空")
    @Size(min = 1, max = 100, message = "数据库名称长度必须在{min}到{max}个字符之间")
    private String databaseName;

    /**
     * 表名称（冗余字段，便于查询）
     */
    @NotBlank(message = "表名称不能为空")
    @Size(min = 1, max = 100, message = "表名称长度必须在{min}到{max}个字符之间")
    private String tableName;

    /**
     * 字段名称
     */
    @NotBlank(message = "字段名称不能为空")
    @Size(min = 1, max = 100, message = "字段名称长度必须在{min}到{max}个字符之间")
    private String columnName;

    /**
     * 字段注释
     */
    @Size(max = 500, message = "字段注释长度不能超过{max}个字符")
    private String columnComment;

    /**
     * 字段在表中的位置（从1开始）
     */
    @NotNull(message = "字段位置不能为空")
    @Min(value = 1, message = "字段位置必须从1开始")
    private Integer ordinalPosition;

    /**
     * 字段默认值
     */
    private String columnDefault;

    /**
     * 是否允许NULL（YES、NO）
     */
    @Pattern(regexp = "^(YES|NO)$", message = "是否允许NULL只能是YES或NO")
    private String isNullable;

    /**
     * 数据类型（varchar、int、bigint等）
     */
    @NotBlank(message = "数据类型不能为空")
    @Size(min = 1, max = 100, message = "数据类型长度必须在{min}到{max}个字符之间")
    private String dataType;

    /**
     * 字符类型最大长度
     */
    @Min(value = 0, message = "字符最大长度不能为负数")
    private Integer characterMaximumLength;

    /**
     * 数值类型精度
     */
    @Min(value = 0, message = "数值精度不能为负数")
    private Integer numericPrecision;

    /**
     * 数值类型小数位数
     */
    @Min(value = 0, message = "小数位数不能为负数")
    private Integer numericScale;

    /**
     * 日期时间类型精度
     */
    @Min(value = 0, message = "日期时间精度不能为负数")
    private Integer datetimePrecision;

    /**
     * 字符集名称
     */
    @Size(max = 50, message = "字符集名称长度不能超过{max}个字符")
    private String characterSetName;

    /**
     * 排序规则名称
     */
    @Size(max = 100, message = "排序规则名称长度不能超过{max}个字符")
    private String collationName;

    /**
     * 完整的字段类型（含长度，如varchar(255)）
     */
    @NotBlank(message = "完整字段类型不能为空")
    @Size(min = 1, max = 200, message = "完整字段类型长度必须在{min}到{max}个字符之间")
    private String columnType;

    /**
     * 键类型（PRI主键、UNI唯一键、MUL普通索引）
     */
    @Size(max = 10, message = "键类型长度不能超过{max}个字符")
    private String columnKey;

    /**
     * 额外信息（auto_increment、on update current_timestamp等）
     */
    @Size(max = 100, message = "额外信息长度不能超过{max}个字符")
    private String extra;

    /**
     * 是否主键（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否主键只能是0或1")
    private String isPrimaryKey;

    /**
     * 是否外键（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否外键只能是0或1")
    private String isForeignKey;

    /**
     * 是否唯一键（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否唯一键只能是0或1")
    private String isUniqueKey;

    /**
     * 是否有索引（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否有索引只能是0或1")
    private String isIndexed;

    /**
     * 外键关联的表名
     */
    @Size(max = 100, message = "外键关联表名长度不能超过{max}个字符")
    private String foreignKeyTable;

    /**
     * 外键关联的字段名
     */
    @Size(max = 100, message = "外键关联字段名长度不能超过{max}个字符")
    private String foreignKeyColumn;

    /**
     * 业务字段名称
     */
    @Size(max = 200, message = "业务字段名称长度不能超过{max}个字符")
    private String businessName;

    /**
     * 业务描述
     */
    @Size(max = 1000, message = "业务描述长度不能超过{max}个字符")
    private String businessDescription;

    /**
     * 数据分类（个人信息、敏感信息、业务信息等）
     */
    @Size(max = 100, message = "数据分类长度不能超过{max}个字符")
    private String dataClassification;

    /**
     * 敏感级别（public公开、internal内部、confidential机密、secret绝密）
     */
    @Pattern(regexp = "^(public|internal|confidential|secret)$", message = "敏感级别只能是public、internal、confidential或secret")
    private String sensitivityLevel;

    /**
     * 是否个人身份信息（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否个人身份信息只能是0或1")
    private String isPii;

    /**
     * 脱敏规则
     */
    @Size(max = 200, message = "脱敏规则长度不能超过{max}个字符")
    private String maskingRule;

    /**
     * 数据验证规则（正则表达式、范围等）
     */
    @Size(max = 500, message = "数据验证规则长度不能超过{max}个字符")
    private String validationRule;

    /**
     * 示例值（JSON数组格式）
     */
    private String sampleValues;

    /**
     * 同步状态（0待同步 1同步成功 2同步失败）
     */
    @Pattern(regexp = "^[012]$", message = "同步状态只能是0、1或2")
    private String syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 状态（0正常 1停用）
     */
    @Pattern(regexp = "^[01]$", message = "状态只能是0或1")
    private String status;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

}