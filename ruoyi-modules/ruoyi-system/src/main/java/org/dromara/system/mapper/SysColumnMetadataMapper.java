package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysColumnMetadata;
import org.dromara.system.domain.vo.SysColumnMetadataVo;

import java.util.List;

/**
 * 字段元数据管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysColumnMetadataMapper extends BaseMapperPlus<SysColumnMetadata, SysColumnMetadataVo> {

    /**
     * 根据表元数据ID查询字段元数据列表
     *
     * @param tableMetaId 表元数据ID
     * @return 字段元数据列表
     */
    default List<SysColumnMetadataVo> selectByTableMetaId(Long tableMetaId) {
        return selectVoList(
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getTableMetaId, tableMetaId)
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getDatabaseName)
                .orderByAsc(SysColumnMetadata::getTableName)
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getDatabaseName, databaseName)
                .eq(SysColumnMetadata::getTableName, tableName)
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getDatabaseName, databaseName)
                .eq(SysColumnMetadata::getTableName, tableName)
                .eq(SysColumnMetadata::getColumnName, columnName)
                .eq(SysColumnMetadata::getDelFlag, SystemConstants.NORMAL)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getSyncStatus, syncStatus)
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getLastSyncTime)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getDatabaseName, databaseName)
                .eq(SysColumnMetadata::getTableName, tableName)
                .eq(SysColumnMetadata::getIsPrimaryKey, "1")
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getDatabaseName, databaseName)
                .eq(SysColumnMetadata::getTableName, tableName)
                .eq(SysColumnMetadata::getIsForeignKey, "1")
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getSensitivityLevel, sensitivityLevel)
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getDatabaseName)
                .orderByAsc(SysColumnMetadata::getTableName)
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getDatasourceId, datasourceId)
                .eq(SysColumnMetadata::getIsPii, "1")
                .eq(SysColumnMetadata::getStatus, "0")
                .orderByAsc(SysColumnMetadata::getDatabaseName)
                .orderByAsc(SysColumnMetadata::getTableName)
                .orderByAsc(SysColumnMetadata::getOrdinalPosition)
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
            new LambdaQueryWrapper<SysColumnMetadata>()
                .eq(SysColumnMetadata::getTableMetaId, tableMetaId)
                .eq(SysColumnMetadata::getStatus, "0")
        );
    }

}