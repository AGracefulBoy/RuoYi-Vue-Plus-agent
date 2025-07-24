package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysTableMetadataBo;
import org.dromara.system.domain.vo.SysTableMetadataVo;

import java.util.Collection;
import java.util.List;

/**
 * 表元数据管理Service接口
 *
 * @author ruoyi
 */
public interface ISysTableMetadataService {

    /**
     * 查询表元数据管理
     *
     * @param tableMetaId 表元数据ID
     * @return 表元数据信息
     */
    SysTableMetadataVo queryById(Long tableMetaId);

    /**
     * 查询表元数据管理列表（分页）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 表元数据列表（分页）
     */
    TableDataInfo<SysTableMetadataVo> queryPageList(SysTableMetadataBo bo, PageQuery pageQuery);

    /**
     * 查询表元数据管理列表
     *
     * @param bo 查询条件
     * @return 表元数据列表
     */
    List<SysTableMetadataVo> queryList(SysTableMetadataBo bo);

    /**
     * 新增表元数据管理
     *
     * @param bo 表元数据信息
     * @return 新增结果
     */
    Boolean insertByBo(SysTableMetadataBo bo);

    /**
     * 修改表元数据管理
     *
     * @param bo 表元数据信息
     * @return 修改结果
     */
    Boolean updateByBo(SysTableMetadataBo bo);

    /**
     * 校验并批量删除表元数据管理信息
     *
     * @param ids 表元数据ID集合
     * @return 删除结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 根据数据源ID查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 表元数据列表
     */
    List<SysTableMetadataVo> queryByDatasourceId(Long datasourceId);

    /**
     * 根据数据源ID查询表元数据列表（分页）
     *
     * @param datasourceId 数据源ID
     * @param pageQuery    分页参数
     * @return 表元数据列表（分页）
     */
    TableDataInfo<SysTableMetadataVo> queryPageByDatasourceId(Long datasourceId, PageQuery pageQuery);

    /**
     * 根据数据源ID和数据库名称查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @return 表元数据列表
     */
    List<SysTableMetadataVo> queryByDatasourceIdAndDatabase(Long datasourceId, String databaseName);

    /**
     * 根据数据源ID、数据库名称和表名称查询表元数据
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @return 表元数据
     */
    SysTableMetadataVo queryByDatasourceIdAndDatabaseAndTable(Long datasourceId, String databaseName, String tableName);

    /**
     * 根据同步状态查询表元数据列表
     *
     * @param syncStatus 同步状态
     * @return 表元数据列表
     */
    List<SysTableMetadataVo> queryBySyncStatus(String syncStatus);

    /**
     * 统计数据源下的表数量
     *
     * @param datasourceId 数据源ID
     * @return 表数量
     */
    long countByDatasourceId(Long datasourceId);

    /**
     * 统计数据库下的表数量
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @return 表数量
     */
    long countByDatasourceIdAndDatabase(Long datasourceId, String databaseName);

    /**
     * 查询需要同步的表元数据列表
     *
     * @param datasourceId 数据源ID
     * @return 需要同步的表元数据列表
     */
    List<SysTableMetadataVo> queryPendingSyncTables(Long datasourceId);

    /**
     * 同步数据源表结构信息
     *
     * @param datasourceId 数据源ID
     * @return 同步结果
     */
    Boolean syncTableStructure(Long datasourceId);

    /**
     * 同步单个表的结构信息
     *
     * @param tableMetaId 表元数据ID
     * @return 同步结果
     */
    Boolean syncSingleTable(Long tableMetaId);

    /**
     * 更新表元数据同步状态
     *
     * @param tableMetaId      表元数据ID
     * @param syncStatus       同步状态
     * @param syncErrorMessage 同步错误信息
     * @return 更新结果
     */
    Boolean updateSyncStatus(Long tableMetaId, String syncStatus, String syncErrorMessage);

    /**
     * 查询表数据（前100条）
     *
     * @param tableMetaId 表元数据ID
     * @return 表数据列表
     */
    List<Object> queryTableData(Long tableMetaId);

}