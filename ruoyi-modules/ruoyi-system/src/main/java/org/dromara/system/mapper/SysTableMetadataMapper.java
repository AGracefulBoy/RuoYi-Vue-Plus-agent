package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysTableMetadata;
import org.dromara.system.domain.vo.SysTableMetadataVo;

import java.util.List;

/**
 * 表元数据管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysTableMetadataMapper extends BaseMapperPlus<SysTableMetadata, SysTableMetadataVo> {

    /**
     * 根据数据源ID查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 表元数据列表
     */
    default List<SysTableMetadataVo> selectByDatasourceId(Long datasourceId) {
        return selectVoList(
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getStatus, "0")
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysTableMetadata::getDatabaseName)
                .orderByAsc(SysTableMetadata::getTableName)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getDatabaseName, databaseName)
                .eq(SysTableMetadata::getStatus, "0")
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysTableMetadata::getTableName)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getDatabaseName, databaseName)
                .eq(SysTableMetadata::getTableName, tableName)
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getSyncStatus, syncStatus)
                .eq(SysTableMetadata::getStatus, "0")
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SysTableMetadata::getLastSyncTime)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getStatus, "0")
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getDatabaseName, databaseName)
                .eq(SysTableMetadata::getStatus, "0")
                .eq(SysTableMetadata::getDelFlag, SystemConstants.NORMAL)
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
            new LambdaQueryWrapper<SysTableMetadata>()
                .eq(SysTableMetadata::getDatasourceId, datasourceId)
                .eq(SysTableMetadata::getSyncStatus, "0")
                .eq(SysTableMetadata::getStatus, "0")
                .orderByAsc(SysTableMetadata::getCreateTime)
        );
    }

}
