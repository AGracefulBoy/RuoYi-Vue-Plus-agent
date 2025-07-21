package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysColumnMetadataBo;
import org.dromara.system.domain.vo.SysColumnMetadataVo;

import java.util.Collection;
import java.util.List;

/**
 * 字段元数据管理Service接口
 *
 * @author ruoyi
 */
public interface ISysColumnMetadataService {

    /**
     * 查询字段元数据管理
     *
     * @param columnMetaId 字段元数据ID
     * @return 字段元数据信息
     */
    SysColumnMetadataVo queryById(Long columnMetaId);

    /**
     * 查询字段元数据管理列表（分页）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 字段元数据列表（分页）
     */
    TableDataInfo<SysColumnMetadataVo> queryPageList(SysColumnMetadataBo bo, PageQuery pageQuery);

    /**
     * 查询字段元数据管理列表
     *
     * @param bo 查询条件
     * @return 字段元数据列表
     */
    List<SysColumnMetadataVo> queryList(SysColumnMetadataBo bo);

    /**
     * 新增字段元数据管理
     *
     * @param bo 字段元数据信息
     * @return 新增结果
     */
    Boolean insertByBo(SysColumnMetadataBo bo);

    /**
     * 修改字段元数据管理
     *
     * @param bo 字段元数据信息
     * @return 修改结果
     */
    Boolean updateByBo(SysColumnMetadataBo bo);

    /**
     * 校验并批量删除字段元数据管理信息
     *
     * @param ids 字段元数据ID集合
     * @return 删除结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 根据表元数据ID查询字段元数据列表
     *
     * @param tableMetaId 表元数据ID
     * @return 字段元数据列表
     */
    List<SysColumnMetadataVo> queryByTableMetaId(Long tableMetaId);

    /**
     * 根据数据源ID查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 字段元数据列表
     */
    List<SysColumnMetadataVo> queryByDatasourceId(Long datasourceId);

    /**
     * 根据数据源ID、数据库名称和表名称查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 字段元数据列表
     */
    List<SysColumnMetadataVo> queryByDatasourceAndTable(Long datasourceId, String databaseName, String tableName);

    /**
     * 根据数据源ID、数据库名称、表名称和字段名称查询字段元数据
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @param columnName   字段名称
     * @return 字段元数据
     */
    SysColumnMetadataVo queryByDatasourceAndTableAndColumn(Long datasourceId, String databaseName, String tableName, String columnName);

    /**
     * 根据同步状态查询字段元数据列表
     *
     * @param syncStatus 同步状态
     * @return 字段元数据列表
     */
    List<SysColumnMetadataVo> queryBySyncStatus(String syncStatus);

    /**
     * 查询主键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 主键字段列表
     */
    List<SysColumnMetadataVo> queryPrimaryKeys(Long datasourceId, String databaseName, String tableName);

    /**
     * 查询外键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 外键字段列表
     */
    List<SysColumnMetadataVo> queryForeignKeys(Long datasourceId, String databaseName, String tableName);

    /**
     * 查询敏感字段列表
     *
     * @param datasourceId     数据源ID
     * @param sensitivityLevel 敏感级别
     * @return 敏感字段列表
     */
    List<SysColumnMetadataVo> querySensitiveColumns(Long datasourceId, String sensitivityLevel);

    /**
     * 查询个人身份信息字段列表
     *
     * @param datasourceId 数据源ID
     * @return 个人身份信息字段列表
     */
    List<SysColumnMetadataVo> queryPiiColumns(Long datasourceId);

    /**
     * 统计表的字段数量
     *
     * @param tableMetaId 表元数据ID
     * @return 字段数量
     */
    long countByTableMetaId(Long tableMetaId);

    /**
     * 批量新增字段元数据
     *
     * @param columnMetadataList 字段元数据列表
     * @return 新增结果
     */
    Boolean batchInsert(List<SysColumnMetadataBo> columnMetadataList);

    /**
     * 同步表的字段结构信息
     *
     * @param tableMetaId 表元数据ID
     * @return 同步结果
     */
    Boolean syncTableColumns(Long tableMetaId);

    /**
     * 更新字段元数据同步状态
     *
     * @param columnMetaId     字段元数据ID
     * @param syncStatus       同步状态
     * @param syncErrorMessage 同步错误信息
     * @return 更新结果
     */
    Boolean updateSyncStatus(Long columnMetaId, String syncStatus, String syncErrorMessage);

    /**
     * 更新字段业务信息
     *
     * @param columnMetaId       字段元数据ID
     * @param businessName       业务字段名称
     * @param businessDescription 业务描述
     * @param dataClassification 数据分类
     * @param sensitivityLevel   敏感级别
     * @param isPii              是否个人身份信息
     * @param maskingRule        脱敏规则
     * @param validationRule     数据验证规则
     * @return 更新结果
     */
    Boolean updateBusinessInfo(Long columnMetaId, String businessName, String businessDescription,
                               String dataClassification, String sensitivityLevel, String isPii,
                               String maskingRule, String validationRule);

}