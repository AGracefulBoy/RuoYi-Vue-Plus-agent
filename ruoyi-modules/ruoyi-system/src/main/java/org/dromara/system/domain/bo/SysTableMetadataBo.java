package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysTableMetadata;

import java.time.LocalDateTime;

/**
 * 表元数据管理业务对象 sys_table_metadata
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTableMetadata.class, reverseConvertGenerate = false)
public class SysTableMetadataBo extends BaseEntity {

    /**
     * 表元数据ID
     */
    @NotNull(message = "表元数据ID不能为空", groups = { EditGroup.class })
    private Long tableMetaId;

    /**
     * 数据源ID
     */
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    /**
     * 数据库名称/Schema名称
     */
    @NotBlank(message = "数据库名称不能为空")
    @Size(min = 1, max = 100, message = "数据库名称长度必须在{min}到{max}个字符之间")
    private String databaseName;

    /**
     * 表名称
     */
    @NotBlank(message = "表名称不能为空")
    @Size(min = 1, max = 100, message = "表名称长度必须在{min}到{max}个字符之间")
    private String tableName;

    /**
     * 表注释
     */
    @Size(max = 500, message = "表注释长度不能超过{max}个字符")
    private String tableComment;

    /**
     * 表类型（TABLE基表、VIEW视图、SYSTEM系统表等）
     */
    @Size(max = 50, message = "表类型长度不能超过{max}个字符")
    private String tableType;

    /**
     * 存储引擎（InnoDB、MyISAM、ClickHouse等）
     */
    @Size(max = 50, message = "存储引擎长度不能超过{max}个字符")
    private String engine;

    /**
     * 字符集
     */
    @Size(max = 50, message = "字符集长度不能超过{max}个字符")
    private String charset;

    /**
     * 排序规则
     */
    @Size(max = 100, message = "排序规则长度不能超过{max}个字符")
    private String collation;

    /**
     * 表行数（估算值）
     */
    @Min(value = 0, message = "表行数不能为负数")
    private Long tableRows;

    /**
     * 数据大小（字节）
     */
    @Min(value = 0, message = "数据大小不能为负数")
    private Long dataLength;

    /**
     * 索引大小（字节）
     */
    @Min(value = 0, message = "索引大小不能为负数")
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
    @Pattern(regexp = "^[012]$", message = "同步状态只能是0、1或2")
    private String syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 同步错误信息
     */
    @Size(max = 1000, message = "同步错误信息长度不能超过{max}个字符")
    private String syncErrorMessage;

    /**
     * 字段数量
     */
    @Min(value = 0, message = "字段数量不能为负数")
    private Integer fieldCount;

    /**
     * 索引数量
     */
    @Min(value = 0, message = "索引数量不能为负数")
    private Integer indexCount;

    /**
     * 是否分区表（0否 1是）
     */
    @Pattern(regexp = "^[01]$", message = "是否分区表只能是0或1")
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
    @Size(max = 1000, message = "业务描述长度不能超过{max}个字符")
    private String businessDescription;

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