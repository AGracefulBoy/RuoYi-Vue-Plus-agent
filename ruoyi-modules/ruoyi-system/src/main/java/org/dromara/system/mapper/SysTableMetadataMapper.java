package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysDatasourceTableMetadata;
import org.dromara.system.domain.vo.SysTableMetadataVo;

import java.util.List;

/**
 * 表元数据管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysTableMetadataMapper extends BaseMapperPlus<SysDatasourceTableMetadata, SysTableMetadataVo> {

    /**
     * 根据数据源ID查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 表元数据列表
     */
    default List<SysTableMetadataVo> selectByDatasourceId(Long datasourceId) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysDatasourceTableMetadata::getDatabaseName)
                .orderByAsc(SysDatasourceTableMetadata::getTableName)
        );
    }

    /**
     * 根据数据源ID和数据库名称查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @return 表元数据列表
     */
    default List<SysTableMetadataVo> selectByDatasourceIdAndDatabase(Long datasourceId, String databaseName) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysDatasourceTableMetadata::getTableName)
        );
    }

    /**
     * 根据数据源ID、数据库名称和表名称查询表元数据
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 表元数据
     */
    default SysTableMetadataVo selectByDatasourceIdAndDatabaseAndTable(Long datasourceId, String databaseName, String tableName) {
        return selectVoOne(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceTableMetadata::getTableName, tableName)
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
        );
    }

    /**
     * 根据同步状态查询表元数据列表
     *
     * @param syncStatus 同步状态
     * @return 表元数据列表
     */
    default List<SysTableMetadataVo> selectBySyncStatus(String syncStatus) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getSyncStatus, syncStatus)
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysDatasourceTableMetadata::getLastSyncTime)
        );
    }

    /**
     * 统计数据源下的表数量
     *
     * @param datasourceId 数据源ID
     * @return 表数量
     */
    default long countByDatasourceId(Long datasourceId) {
        return selectCount(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
        );
    }

    /**
     * 统计数据库下的表数量
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @return 表数量
     */
    default long countByDatasourceIdAndDatabase(Long datasourceId, String databaseName) {
        return selectCount(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getDatabaseName, databaseName)
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .eq(SysDatasourceTableMetadata::getDelFlag, SystemConstants.NORMAL)
        );
    }

    /**
     * 查询需要同步的表元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 需要同步的表元数据列表
     */
    default List<SysTableMetadataVo> selectPendingSyncTables(Long datasourceId) {
        return selectVoList(
            new LambdaQueryWrapper<SysDatasourceTableMetadata>()
                .eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getSyncStatus, "0")
                .eq(SysDatasourceTableMetadata::getStatus, "0")
                .orderByAsc(SysDatasourceTableMetadata::getCreateTime)
        );
    }

}
