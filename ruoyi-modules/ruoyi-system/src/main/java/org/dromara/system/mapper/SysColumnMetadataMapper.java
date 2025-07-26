package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysDatasourceColumnMetadata;
import org.dromara.system.domain.vo.SysColumnMetadataVo;

import java.util.List;

/**
 * 字段元数据管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysColumnMetadataMapper extends BaseMapperPlus<SysDatasourceColumnMetadata, SysColumnMetadataVo> {

    /**
     * 根据表元数据ID查询字段元数据列表
     *
     * @param tableMetaId 表元数据ID
     * @return 字段元数据列表
     */
    default List<SysColumnMetadataVo> selectByTableMetaId(Long tableMetaId) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getTableMetaId, tableMetaId)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 根据数据源ID查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 字段元数据列表
     */
    default List<SysColumnMetadataVo> selectByDatasourceId(Long datasourceId) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getDatabaseName)
                .orderByAsc(SysDatasourceColumnMetadata::getTableName)
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 根据数据源ID、数据库名称和表名称查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 字段元数据列表
     */
    default List<SysColumnMetadataVo> selectByDatasourceAndTable(Long datasourceId, String databaseName, String tableName) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceColumnMetadata::getTableName, tableName)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 根据数据源ID、数据库名称、表名称和字段名称查询字段元数据
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @param columnName   字段名称
     * @return 字段元数据
     */
    default SysColumnMetadataVo selectByDatasourceAndTableAndColumn(Long datasourceId, String databaseName, String tableName, String columnName) {
        return selectVoOne(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceColumnMetadata::getTableName, tableName)
                .eq(SysDatasourceColumnMetadata::getColumnName, columnName)
                .eq(SysDatasourceColumnMetadata::getDelFlag, SystemConstants.NORMAL)
        );
    }

    /**
     * 根据同步状态查询字段元数据列表
     *
     * @param syncStatus 同步状态
     * @return 字段元数据列表
     */
    default List<SysColumnMetadataVo> selectBySyncStatus(String syncStatus) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getSyncStatus, syncStatus)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getLastSyncTime)
        );
    }

    /**
     * 查询主键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 主键字段列表
     */
    default List<SysColumnMetadataVo> selectPrimaryKeys(Long datasourceId, String databaseName, String tableName) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceColumnMetadata::getTableName, tableName)
                .eq(SysDatasourceColumnMetadata::getIsPrimaryKey, "1")
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 查询外键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 外键字段列表
     */
    default List<SysColumnMetadataVo> selectForeignKeys(Long datasourceId, String databaseName, String tableName) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceColumnMetadata::getTableName, tableName)
                .eq(SysDatasourceColumnMetadata::getIsForeignKey, "1")
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 查询敏感字段列表
     *
     * @param datasourceId    数据源ID
     * @param sensitivityLevel 敏感级别
     * @return 敏感字段列表
     */
    default List<SysColumnMetadataVo> selectSensitiveColumns(Long datasourceId, String sensitivityLevel) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getSensitivityLevel, sensitivityLevel)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getDatabaseName)
                .orderByAsc(SysDatasourceColumnMetadata::getTableName)
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 查询个人身份信息字段列表
     *
     * @param datasourceId 数据源ID
     * @return 个人身份信息字段列表
     */
    default List<SysColumnMetadataVo> selectPiiColumns(Long datasourceId) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceColumnMetadata::getIsPii, "1")
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceColumnMetadata::getDatabaseName)
                .orderByAsc(SysDatasourceColumnMetadata::getTableName)
                .orderByAsc(SysDatasourceColumnMetadata::getOrdinalPosition)
        );
    }

    /**
     * 统计表的字段数量
     *
     * @param tableMetaId 表元数据ID
     * @return 字段数量
     */
    default long countByTableMetaId(Long tableMetaId) {
        return selectCount(
            new LambdaQueryWrapper<SysDatasourceColumnMetadata>()
                .eq(SysDatasourceColumnMetadata::getTableMetaId, tableMetaId)
                .eq(SysDatasourceColumnMetadata::getStatus, "0")
        );
    }

}
