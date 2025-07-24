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
import org.dromara.system.domain.SysTableMetadata;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysTableMetadataBo;
import org.dromara.system.domain.vo.SysTableMetadataVo;
import org.dromara.system.mapper.SysTableMetadataMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysTableMetadataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
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
        LambdaQueryWrapper<SysTableMetadata> lqw = buildQueryWrapper(bo);
        Page<SysTableMetadata> page = pageQuery.build();
        IPage<SysTableMetadataVo> result = baseMapper.selectVoPage(page, lqw);
        
        // 填充关联数据
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            fillRelatedData(result.getRecords());
        }
        
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysTableMetadataVo> queryList(SysTableMetadataBo bo) {
        LambdaQueryWrapper<SysTableMetadata> lqw = buildQueryWrapper(bo);
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

    private LambdaQueryWrapper<SysTableMetadata> buildQueryWrapper(SysTableMetadataBo bo) {
        LambdaQueryWrapper<SysTableMetadata> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getDatasourceId()), SysTableMetadata::getDatasourceId, bo.getDatasourceId());
        lqw.like(StringUtils.isNotBlank(bo.getDatabaseName()), SysTableMetadata::getDatabaseName, bo.getDatabaseName());
        lqw.like(StringUtils.isNotBlank(bo.getTableName()), SysTableMetadata::getTableName, bo.getTableName());
        lqw.like(StringUtils.isNotBlank(bo.getTableComment()), SysTableMetadata::getTableComment, bo.getTableComment());
        lqw.eq(StringUtils.isNotBlank(bo.getTableType()), SysTableMetadata::getTableType, bo.getTableType());
        lqw.eq(StringUtils.isNotBlank(bo.getEngine()), SysTableMetadata::getEngine, bo.getEngine());
        lqw.eq(StringUtils.isNotBlank(bo.getSyncStatus()), SysTableMetadata::getSyncStatus, bo.getSyncStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getIsPartitioned()), SysTableMetadata::getIsPartitioned, bo.getIsPartitioned());
        lqw.like(StringUtils.isNotBlank(bo.getBusinessDescription()), SysTableMetadata::getBusinessDescription, bo.getBusinessDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysTableMetadata::getStatus, bo.getStatus());
        lqw.between(ObjectUtil.isAllNotEmpty(bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime")),
                SysTableMetadata::getCreateTime, bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime"));
        lqw.orderByDesc(SysTableMetadata::getLastSyncTime);
        lqw.orderByDesc(SysTableMetadata::getCreateTime);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysTableMetadataBo bo) {
        SysTableMetadata add = MapstructUtils.convert(bo, SysTableMetadata.class);
        
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
        SysTableMetadata update = MapstructUtils.convert(bo, SysTableMetadata.class);
        return baseMapper.updateById(update) > 0;
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
    public TableDataInfo<SysTableMetadataVo> queryPageByDatasourceId(Long datasourceId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTableMetadata> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTableMetadata::getDatasourceId, datasourceId);
        lqw.orderByDesc(SysTableMetadata::getCreateTime);
        
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
            LambdaUpdateWrapper<SysTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysTableMetadata::getDatasourceId, datasourceId)
                         .eq(SysTableMetadata::getSyncStatus, "0")
                         .set(SysTableMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysTableMetadata::getSyncStatus, "1");
            
            baseMapper.update(null, updateWrapper);
            
            log.info("同步数据源表结构信息完成，数据源ID：{}", datasourceId);
            return true;
        } catch (Exception e) {
            log.error("同步数据源表结构信息失败，数据源ID：{}，错误信息：{}", datasourceId, e.getMessage(), e);
            
            // 更新同步状态为失败
            LambdaUpdateWrapper<SysTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysTableMetadata::getDatasourceId, datasourceId)
                         .eq(SysTableMetadata::getSyncStatus, "0")
                         .set(SysTableMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysTableMetadata::getSyncStatus, "2")
                         .set(SysTableMetadata::getSyncErrorMessage, e.getMessage());
            
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
            LambdaUpdateWrapper<SysTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysTableMetadata::getTableMetaId, tableMetaId)
                         .set(SysTableMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysTableMetadata::getSyncStatus, "1")
                         .set(SysTableMetadata::getSyncErrorMessage, null);
            
            baseMapper.update(null, updateWrapper);
            
            log.info("同步单个表结构信息完成，表元数据ID：{}", tableMetaId);
            return true;
        } catch (Exception e) {
            log.error("同步单个表结构信息失败，表元数据ID：{}，错误信息：{}", tableMetaId, e.getMessage(), e);
            
            // 更新同步状态为失败
            LambdaUpdateWrapper<SysTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysTableMetadata::getTableMetaId, tableMetaId)
                         .set(SysTableMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysTableMetadata::getSyncStatus, "2")
                         .set(SysTableMetadata::getSyncErrorMessage, e.getMessage());
            
            baseMapper.update(null, updateWrapper);
            
            throw new ServiceException("同步表结构信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSyncStatus(Long tableMetaId, String syncStatus, String syncErrorMessage) {
        LambdaUpdateWrapper<SysTableMetadata> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysTableMetadata::getTableMetaId, tableMetaId)
                     .set(SysTableMetadata::getLastSyncTime, LocalDateTime.now())
                     .set(SysTableMetadata::getSyncStatus, syncStatus);
        
        if (StringUtils.isNotBlank(syncErrorMessage)) {
            updateWrapper.set(SysTableMetadata::getSyncErrorMessage, syncErrorMessage);
        } else {
            updateWrapper.set(SysTableMetadata::getSyncErrorMessage, null);
        }
        
        return baseMapper.update(null, updateWrapper) > 0;
    }

}