package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysDatasourceColumnMetadata;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段元数据管理视图对象 sys_column_metadata
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDatasourceColumnMetadata.class)
public class SysColumnMetadataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字段元数据ID
     */
    @ExcelProperty(value = "字段元数据ID")
    private Long columnMetaId;

    /**
     * 租户编号
     */
    @ExcelProperty(value = "租户编号")
    private String tenantId;

    /**
     * 表元数据ID
     */
    @ExcelProperty(value = "表元数据ID")
    private Long tableMetaId;

    /**
     * 数据源ID（冗余字段，便于查询）
     */
    @ExcelProperty(value = "数据源ID")
    private Long datasourceId;

    /**
     * 数据源名称
     */
    @ExcelProperty(value = "数据源名称")
    private String datasourceName;

    /**
     * 数据库名称（冗余字段，便于查询）
     */
    @ExcelProperty(value = "数据库名称")
    private String databaseName;

    /**
     * 表名称（冗余字段，便于查询）
     */
    @ExcelProperty(value = "表名称")
    private String tableName;

    /**
     * 字段名称
     */
    @ExcelProperty(value = "字段名称")
    private String columnName;

    /**
     * 字段注释
     */
    @ExcelProperty(value = "字段注释")
    private String columnComment;

    /**
     * 字段描述
     */
    @ExcelProperty(value = "字段描述")
    private String columnDesc;

    /**
     * 字段在表中的位置（从1开始）
     */
    @ExcelProperty(value = "字段位置")
    private Integer ordinalPosition;

    /**
     * 字段默认值
     */
    @ExcelProperty(value = "默认值")
    private String columnDefault;

    /**
     * 是否允许NULL（YES、NO）
     */
    @ExcelProperty(value = "允许NULL", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "YES=是,NO=否")
    private String isNullable;

    /**
     * 数据类型（varchar、int、bigint等）
     */
    @ExcelProperty(value = "数据类型")
    private String dataType;

    /**
     * 字符类型最大长度
     */
    @ExcelProperty(value = "最大长度")
    private Integer characterMaximumLength;

    /**
     * 数值类型精度
     */
    @ExcelProperty(value = "数值精度")
    private Integer numericPrecision;

    /**
     * 数值类型小数位数
     */
    @ExcelProperty(value = "小数位数")
    private Integer numericScale;

    /**
     * 日期时间类型精度
     */
    @ExcelProperty(value = "时间精度")
    private Integer datetimePrecision;

    /**
     * 字符集名称
     */
    @ExcelProperty(value = "字符集")
    private String characterSetName;

    /**
     * 排序规则名称
     */
    @ExcelProperty(value = "排序规则")
    private String collationName;

    /**
     * 完整的字段类型（含长度，如varchar(255)）
     */
    @ExcelProperty(value = "完整类型")
    private String columnType;

    /**
     * 键类型（PRI主键、UNI唯一键、MUL普通索引）
     */
    @ExcelProperty(value = "键类型")
    private String columnKey;

    /**
     * 额外信息（auto_increment、on update current_timestamp等）
     */
    @ExcelProperty(value = "额外信息")
    private String extra;

    /**
     * 是否主键（0否 1是）
     */
    @ExcelProperty(value = "是否主键", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isPrimaryKey;

    /**
     * 是否外键（0否 1是）
     */
    @ExcelProperty(value = "是否外键", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isForeignKey;

    /**
     * 是否唯一键（0否 1是）
     */
    @ExcelProperty(value = "是否唯一键", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isUniqueKey;

    /**
     * 是否有索引（0否 1是）
     */
    @ExcelProperty(value = "是否有索引", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isIndexed;

    /**
     * 外键关联的表名
     */
    @ExcelProperty(value = "外键表名")
    private String foreignKeyTable;

    /**
     * 外键关联的字段名
     */
    @ExcelProperty(value = "外键字段名")
    private String foreignKeyColumn;

    /**
     * 业务字段名称
     */
    @ExcelProperty(value = "业务字段名")
    private String businessName;

    /**
     * 业务描述
     */
    @ExcelProperty(value = "业务描述")
    private String businessDescription;

    /**
     * 数据分类（个人信息、敏感信息、业务信息等）
     */
    @ExcelProperty(value = "数据分类")
    private String dataClassification;

    /**
     * 敏感级别（public公开、internal内部、confidential机密、secret绝密）
     */
    @ExcelProperty(value = "敏感级别", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "public=公开,internal=内部,confidential=机密,secret=绝密")
    private String sensitivityLevel;

    /**
     * 是否个人身份信息（0否 1是）
     */
    @ExcelProperty(value = "是否个人信息", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
    private String isPii;

    /**
     * 脱敏规则
     */
    @ExcelProperty(value = "脱敏规则")
    private String maskingRule;

    /**
     * 数据验证规则（正则表达式、范围等）
     */
    @ExcelProperty(value = "验证规则")
    private String validationRule;

    /**
     * 示例值（JSON数组格式）
     */
    private String sampleValues;

    /**
     * 同步状态（0待同步 1同步成功 2同步失败）
     */
    @ExcelProperty(value = "同步状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=待同步,1=同步成功,2=同步失败")
    private String syncStatus;

    /**
     * 最后同步时间
     */
    @ExcelProperty(value = "最后同步时间")
    private LocalDateTime lastSyncTime;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

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
