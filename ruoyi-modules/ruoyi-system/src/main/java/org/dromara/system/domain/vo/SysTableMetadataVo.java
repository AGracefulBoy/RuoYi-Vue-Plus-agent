package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysDatasourceTableMetadata;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 表元数据管理视图对象 sys_table_metadata
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysDatasourceTableMetadata.class)
public class SysTableMetadataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表元数据ID
     */
    @ExcelProperty(value = "表元数据ID")
    private Long tableMetaId;

    /**
     * 租户编号
     */
    @ExcelProperty(value = "租户编号")
    private String tenantId;

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
     * 数据库名称/Schema名称
     */
    @ExcelProperty(value = "数据库名称")
    private String databaseName;

    /**
     * 表名称
     */
    @ExcelProperty(value = "表名称")
    private String tableName;

    /**
     * 表注释
     */
    @ExcelProperty(value = "表注释")
    private String tableComment;

    /**
     * 表描述
     */
    @ExcelProperty(value = "表描述")
    private String tableDesc;

    /**
     * 表类型（TABLE基表、VIEW视图、SYSTEM系统表等）
     */
    @ExcelProperty(value = "表类型")
    private String tableType;

    /**
     * 存储引擎（InnoDB、MyISAM、ClickHouse等）
     */
    @ExcelProperty(value = "存储引擎")
    private String engine;

    /**
     * 字符集
     */
    @ExcelProperty(value = "字符集")
    private String charset;

    /**
     * 排序规则
     */
    @ExcelProperty(value = "排序规则")
    private String collation;

    /**
     * 表行数（估算值）
     */
    @ExcelProperty(value = "表行数")
    private Long tableRows;

    /**
     * 数据大小（字节）
     */
    @ExcelProperty(value = "数据大小")
    private Long dataLength;

    /**
     * 索引大小（字节）
     */
    @ExcelProperty(value = "索引大小")
    private Long indexLength;

    /**
     * 表在数据库中的创建时间
     */
    @ExcelProperty(value = "表创建时间")
    private LocalDateTime createTimeDb;

    /**
     * 表在数据库中的更新时间
     */
    @ExcelProperty(value = "表更新时间")
    private LocalDateTime updateTimeDb;

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
     * 同步错误信息
     */
    private String syncErrorMessage;

    /**
     * 字段数量
     */
    @ExcelProperty(value = "字段数量")
    private Integer fieldCount;

    /**
     * 索引数量
     */
    @ExcelProperty(value = "索引数量")
    private Integer indexCount;

    /**
     * 是否分区表（0否 1是）
     */
    @ExcelProperty(value = "是否分区表", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=否,1=是")
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
    @ExcelProperty(value = "业务描述")
    private String businessDescription;

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
