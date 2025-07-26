package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysDatasourceTableMetadata;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysTableMetadataBo;
import org.dromara.system.domain.bo.SysDatasourceBo;
import org.dromara.system.domain.bo.SysTableDescUpdateBo;
import org.dromara.system.domain.bo.SysColumnDescUpdateBo;
import org.dromara.system.domain.vo.SysTableMetadataVo;
import org.dromara.system.mapper.SysTableMetadataMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysTableMetadataService;
import org.dromara.system.service.ISysColumnMetadataService;
import org.dromara.system.util.DatasourceValidatorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 表元数据管理Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysTableMetadataServiceImpl implements ISysTableMetadataService {

    private final SysTableMetadataMapper baseMapper;
    private final SysDatasourceMapper datasourceMapper;
    private final SysUserMapper userMapper;
    private final ISysColumnMetadataService columnMetadataService;

    @Override
    public SysTableMetadataVo queryById(Long tableMetaId) {
        SysTableMetadataVo vo = baseMapper.selectVoById(tableMetaId);
        if (vo != null) {
            // 填充关联数据
            fillRelatedData(List.of(vo));
        }
        return vo;
    }

    @Override
    public TableDataInfo<SysTableMetadataVo> queryPageList(SysTableMetadataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasourceTableMetadata> lqw = buildQueryWrapper(bo);
        Page<SysDatasourceTableMetadata> page = pageQuery.build();
        IPage<SysTableMetadataVo> result = baseMapper.selectVoPage(page, lqw);

        // 填充关联数据
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            fillRelatedData(result.getRecords());
        }

        return TableDataInfo.build(result);
    }

    @Override
    public List<SysTableMetadataVo> queryList(SysTableMetadataBo bo) {
        LambdaQueryWrapper<SysDatasourceTableMetadata> lqw = buildQueryWrapper(bo);
        List<SysTableMetadataVo> list = baseMapper.selectVoList(lqw);

        // 填充关联数据
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }

        return list;
    }

    /**
     * 填充关联数据（数据源名称、创建者名称、更新者名称）
     *
     * @param list 表元数据列表
     */
    private void fillRelatedData(List<SysTableMetadataVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // 收集所有需要查询的ID
        Set<Long> datasourceIds = list.stream()
            .map(SysTableMetadataVo::getDatasourceId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());

        Set<Long> userIds = list.stream()
            .flatMap(vo -> List.of(vo.getCreateBy(), vo.getUpdateBy()).stream())
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());

        // 批量查询数据源名称
        Map<Long, String> datasourceNameMap = Map.of();
        if (!datasourceIds.isEmpty()) {
            List<SysDatasource> datasources = datasourceMapper.selectList(
                new LambdaQueryWrapper<SysDatasource>()
                    .in(SysDatasource::getDatasourceId, datasourceIds)
                    .eq(SysDatasource::getTenantId, "000000")
                    .eq(SysDatasource::getDelFlag, SystemConstants.NORMAL)
                    .select(SysDatasource::getDatasourceId, SysDatasource::getDatasourceName)
            );
            datasourceNameMap = datasources.stream()
                .collect(Collectors.toMap(
                    SysDatasource::getDatasourceId,
                    SysDatasource::getDatasourceName));
        }

        // 批量查询用户名称
        Map<Long, String> userNameMap = Map.of();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                    .in(SysUser::getUserId, userIds)
                    .eq(SysUser::getTenantId, "000000")
                    .eq(SysUser::getDelFlag, SystemConstants.NORMAL)
                    .select(SysUser::getUserId, SysUser::getNickName)
            );
            userNameMap = users.stream()
                .collect(Collectors.toMap(
                    SysUser::getUserId,
                    SysUser::getNickName));
        }

        // 填充数据
        final Map<Long, String> finalDatasourceNameMap = datasourceNameMap;
        final Map<Long, String> finalUserNameMap = userNameMap;

        list.forEach(vo -> {
            // 填充数据源名称
            if (vo.getDatasourceId() != null) {
                vo.setDatasourceName(finalDatasourceNameMap.get(vo.getDatasourceId()));
            }

            // 填充创建者名称
            if (vo.getCreateBy() != null) {
                vo.setCreateByName(finalUserNameMap.get(vo.getCreateBy()));
            }

            // 填充更新者名称
            if (vo.getUpdateBy() != null) {
                vo.setUpdateByName(finalUserNameMap.get(vo.getUpdateBy()));
            }
        });
    }

    private LambdaQueryWrapper<SysDatasourceTableMetadata> buildQueryWrapper(SysTableMetadataBo bo) {
        LambdaQueryWrapper<SysDatasourceTableMetadata> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getDatasourceId()), SysDatasourceTableMetadata::getDatasourceId, bo.getDatasourceId());
        lqw.like(StringUtils.isNotBlank(bo.getDatabaseName()), SysDatasourceTableMetadata::getDatabaseName, bo.getDatabaseName());
        lqw.like(StringUtils.isNotBlank(bo.getTableName()), SysDatasourceTableMetadata::getTableName, bo.getTableName());
        lqw.like(StringUtils.isNotBlank(bo.getTableComment()), SysDatasourceTableMetadata::getTableComment, bo.getTableComment());
        lqw.like(StringUtils.isNotBlank(bo.getTableDesc()), SysDatasourceTableMetadata::getTableDesc, bo.getTableDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getTableType()), SysDatasourceTableMetadata::getTableType, bo.getTableType());
        lqw.eq(StringUtils.isNotBlank(bo.getEngine()), SysDatasourceTableMetadata::getEngine, bo.getEngine());
        lqw.eq(StringUtils.isNotBlank(bo.getSyncStatus()), SysDatasourceTableMetadata::getSyncStatus, bo.getSyncStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getIsPartitioned()), SysDatasourceTableMetadata::getIsPartitioned, bo.getIsPartitioned());
        lqw.like(StringUtils.isNotBlank(bo.getBusinessDescription()), SysDatasourceTableMetadata::getBusinessDescription, bo.getBusinessDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysDatasourceTableMetadata::getStatus, bo.getStatus());
        lqw.between(ObjectUtil.isAllNotEmpty(bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime")),
            SysDatasourceTableMetadata::getCreateTime, bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime"));
        lqw.orderByDesc(SysDatasourceTableMetadata::getLastSyncTime);
        lqw.orderByDesc(SysDatasourceTableMetadata::getCreateTime);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTableMetadataBo bo) {
        SysDatasourceTableMetadata add = MapstructUtils.convert(bo, SysDatasourceTableMetadata.class);

        // 检查是否已存在相同的表
        SysTableMetadataVo existingTable = baseMapper.selectByDatasourceIdAndDatabaseAndTable(
            bo.getDatasourceId(), bo.getDatabaseName(), bo.getTableName());
        if (ObjectUtil.isNotNull(existingTable)) {
            throw new ServiceException("表元数据已存在：" + bo.getDatabaseName() + "." + bo.getTableName());
        }

        // 设置默认值
        if (StringUtils.isBlank(add.getSyncStatus())) {
            add.setSyncStatus("0");
        }
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("0");
        }
        if (StringUtils.isBlank(add.getIsPartitioned())) {
            add.setIsPartitioned("0");
        }
        if (StringUtils.isBlank(add.getTableType())) {
            add.setTableType("TABLE");
        }

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setTableMetaId(add.getTableMetaId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysTableMetadataBo bo) {
        SysDatasourceTableMetadata update = MapstructUtils.convert(bo, SysDatasourceTableMetadata.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTableDesc(Long tableMetaId, String tableDesc) {
        // 验证表元数据是否存在
        SysDatasourceTableMetadata existing = baseMapper.selectById(tableMetaId);
        if (ObjectUtil.isNull(existing)) {
            throw new ServiceException("表元数据不存在");
        }

        // 创建更新对象，只设置需要更新的字段
        SysDatasourceTableMetadata update = new SysDatasourceTableMetadata();
        update.setTableMetaId(tableMetaId);
        update.setTableDesc(tableDesc);

        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTableAndColumnDesc(SysTableDescUpdateBo bo) {
        // 更新表描述
        Boolean tableUpdateResult = updateTableDesc(bo.getTableMetaId(), bo.getTableDesc());

        // 更新字段描述（如果有提供）
        if (ObjectUtil.isNotEmpty(bo.getSysColumnDescUpdateBoList())) {
            for (SysColumnDescUpdateBo columnBo : bo.getSysColumnDescUpdateBoList()) {
                Boolean columnUpdateResult = columnMetadataService.updateColumnDesc(
                    columnBo.getColumnMetaId(),
                    columnBo.getColumnDesc()
                );
                if (!columnUpdateResult) {
                    throw new ServiceException("更新字段描述失败：" + columnBo.getColumnName());
                }
            }
        }

        return tableUpdateResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return false;
        }

        // 可以在这里添加删除前的验证逻辑，比如检查是否有关联的字段元数据
        for (Long id : ids) {
            SysTableMetadataVo tableMetadata = baseMapper.selectVoById(id);
            if (ObjectUtil.isNotNull(tableMetadata) && tableMetadata.getFieldCount() != null && tableMetadata.getFieldCount() > 0) {
                throw new ServiceException("表元数据存在关联字段，不允许删除：" + tableMetadata.getTableName());
            }
        }

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<SysTableMetadataVo> queryByDatasourceId(Long datasourceId) {
        List<SysTableMetadataVo> list = baseMapper.selectByDatasourceId(datasourceId);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public TableDataInfo<SysTableMetadataVo> queryPageByDatasourceId(Long datasourceId, String keyword, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasourceTableMetadata> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId);
        lqw.like(StringUtils.isNotBlank(keyword), SysDatasourceTableMetadata::getTableName, keyword);
        lqw.like(StringUtils.isNotBlank(keyword), SysDatasourceTableMetadata::getTableComment, keyword);
        lqw.orderByDesc(SysDatasourceTableMetadata::getCreateTime);

        Page<SysTableMetadataVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        List<SysTableMetadataVo> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            fillRelatedData(records);
        }
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysTableMetadataVo> queryByDatasourceIdAndDatabase(Long datasourceId, String databaseName) {
        List<SysTableMetadataVo> list = baseMapper.selectByDatasourceIdAndDatabase(datasourceId, databaseName);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public SysTableMetadataVo queryByDatasourceIdAndDatabaseAndTable(Long datasourceId, String databaseName, String tableName) {
        SysTableMetadataVo vo = baseMapper.selectByDatasourceIdAndDatabaseAndTable(datasourceId, databaseName, tableName);
        if (vo != null) {
            fillRelatedData(List.of(vo));
        }
        return vo;
    }

    @Override
    public List<SysTableMetadataVo> queryBySyncStatus(String syncStatus) {
        List<SysTableMetadataVo> list = baseMapper.selectBySyncStatus(syncStatus);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public long countByDatasourceId(Long datasourceId) {
        return baseMapper.countByDatasourceId(datasourceId);
    }

    @Override
    public long countByDatasourceIdAndDatabase(Long datasourceId, String databaseName) {
        return baseMapper.countByDatasourceIdAndDatabase(datasourceId, databaseName);
    }

    @Override
    public List<SysTableMetadataVo> queryPendingSyncTables(Long datasourceId) {
        List<SysTableMetadataVo> list = baseMapper.selectPendingSyncTables(datasourceId);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncTableStructure(Long datasourceId) {
        log.info("开始同步数据源表结构信息，数据源ID：{}", datasourceId);

        try {
            // TODO: 这里应该实现具体的同步逻辑
            // 1. 获取数据源连接信息
            // 2. 连接数据库获取表结构信息
            // 3. 更新或插入表元数据信息
            // 4. 更新同步状态和时间

            // 暂时只更新同步时间作为示例
            LambdaUpdateWrapper<SysDatasourceTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getSyncStatus, "0")
                .set(SysDatasourceTableMetadata::getLastSyncTime, LocalDateTime.now())
                .set(SysDatasourceTableMetadata::getSyncStatus, "1");

            baseMapper.update(null, updateWrapper);

            log.info("同步数据源表结构信息完成，数据源ID：{}", datasourceId);
            return true;
        } catch (Exception e) {
            log.error("同步数据源表结构信息失败，数据源ID：{}，错误信息：{}", datasourceId, e.getMessage(), e);

            // 更新同步状态为失败
            LambdaUpdateWrapper<SysDatasourceTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysDatasourceTableMetadata::getDatasourceId, datasourceId)
                .eq(SysDatasourceTableMetadata::getSyncStatus, "0")
                .set(SysDatasourceTableMetadata::getLastSyncTime, LocalDateTime.now())
                .set(SysDatasourceTableMetadata::getSyncStatus, "2")
                .set(SysDatasourceTableMetadata::getSyncErrorMessage, e.getMessage());

            baseMapper.update(null, updateWrapper);

            throw new ServiceException("同步表结构信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncSingleTable(Long tableMetaId) {
        log.info("开始同步单个表结构信息，表元数据ID：{}", tableMetaId);

        try {
            SysTableMetadataVo tableMetadata = baseMapper.selectVoById(tableMetaId);
            if (ObjectUtil.isNull(tableMetadata)) {
                throw new ServiceException("表元数据不存在");
            }

            // TODO: 这里应该实现具体的单表同步逻辑
            // 1. 获取表的最新结构信息
            // 2. 更新表元数据信息
            // 3. 更新同步状态和时间

            // 暂时只更新同步时间作为示例
            LambdaUpdateWrapper<SysDatasourceTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysDatasourceTableMetadata::getTableMetaId, tableMetaId)
                .set(SysDatasourceTableMetadata::getLastSyncTime, LocalDateTime.now())
                .set(SysDatasourceTableMetadata::getSyncStatus, "1")
                .set(SysDatasourceTableMetadata::getSyncErrorMessage, null);

            baseMapper.update(null, updateWrapper);

            log.info("同步单个表结构信息完成，表元数据ID：{}", tableMetaId);
            return true;
        } catch (Exception e) {
            log.error("同步单个表结构信息失败，表元数据ID：{}，错误信息：{}", tableMetaId, e.getMessage(), e);

            // 更新同步状态为失败
            LambdaUpdateWrapper<SysDatasourceTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysDatasourceTableMetadata::getTableMetaId, tableMetaId)
                .set(SysDatasourceTableMetadata::getLastSyncTime, LocalDateTime.now())
                .set(SysDatasourceTableMetadata::getSyncStatus, "2")
                .set(SysDatasourceTableMetadata::getSyncErrorMessage, e.getMessage());

            baseMapper.update(null, updateWrapper);

            throw new ServiceException("同步表结构信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSyncStatus(Long tableMetaId, String syncStatus, String syncErrorMessage) {
        LambdaUpdateWrapper<SysDatasourceTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysDatasourceTableMetadata::getTableMetaId, tableMetaId)
            .set(SysDatasourceTableMetadata::getLastSyncTime, LocalDateTime.now())
            .set(SysDatasourceTableMetadata::getSyncStatus, syncStatus);

        if (StringUtils.isNotBlank(syncErrorMessage)) {
            updateWrapper.set(SysDatasourceTableMetadata::getSyncErrorMessage, syncErrorMessage);
        } else {
            updateWrapper.set(SysDatasourceTableMetadata::getSyncErrorMessage, null);
        }

        return baseMapper.update(null, updateWrapper) > 0;
    }

    @Override
    public List<Object> queryTableData(Long tableMetaId) {
        log.info("查询表数据，表元数据ID：{}", tableMetaId);

        List<Object> result = new ArrayList<>();

        try {
            // 获取表元数据信息
            SysTableMetadataVo tableMetadata = queryById(tableMetaId);
            if (ObjectUtil.isNull(tableMetadata)) {
                throw new ServiceException("表元数据不存在");
            }

            // 获取数据源信息
            SysDatasource datasource = datasourceMapper.selectById(tableMetadata.getDatasourceId());
            if (ObjectUtil.isNull(datasource)) {
                throw new ServiceException("数据源不存在");
            }

            // 构建连接URL
            SysDatasourceBo datasourceBo = MapstructUtils.convert(datasource, SysDatasourceBo.class);
            String connectionUrl = DatasourceValidatorUtils.buildConnectionUrl(datasourceBo);

            // 添加连接参数
            if (StringUtils.isNotBlank(datasource.getConnectionParams())) {
                connectionUrl += (connectionUrl.contains("?") ? "&" : "?") + datasource.getConnectionParams();
            }


            if (StringUtils.isNotBlank(datasource.getDriverClassName())) {
                Class.forName(datasource.getDriverClassName());
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }

            // 建立连接并查询数据
            try (Connection connection = DriverManager.getConnection(
                connectionUrl,
                datasource.getUsername(),
                datasource.getPassword())) {

                // 构建查询SQL，限制100条
                String sql = String.format("SELECT * FROM `%s`.`%s` LIMIT 100",
                    tableMetadata.getDatabaseName(), tableMetadata.getTableName());

                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {

                    // 获取元数据信息
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // 遍历结果集
                    while (resultSet.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = resultSet.getObject(i);
                            row.put(columnName, value);
                        }
                        result.add(row);
                    }
                }
            }

            log.info("查询表数据成功，返回记录数：{}", result.size() - 1);
            return result;

        } catch (Exception e) {
            log.error("查询表数据失败：{}", e.getMessage(), e);
            throw new ServiceException("查询表数据失败：" + e.getMessage());
        }
    }

}
