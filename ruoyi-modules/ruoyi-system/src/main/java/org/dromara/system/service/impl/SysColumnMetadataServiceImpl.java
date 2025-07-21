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
import org.dromara.system.domain.SysColumnMetadata;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysColumnMetadataBo;
import org.dromara.system.domain.vo.SysColumnMetadataVo;
import org.dromara.system.mapper.SysColumnMetadataMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysColumnMetadataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字段元数据管理Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysColumnMetadataServiceImpl implements ISysColumnMetadataService {

    private final SysColumnMetadataMapper baseMapper;
    private final SysDatasourceMapper datasourceMapper;
    private final SysUserMapper userMapper;

    @Override
    public SysColumnMetadataVo queryById(Long columnMetaId) {
        SysColumnMetadataVo vo = baseMapper.selectVoById(columnMetaId);
        if (vo != null) {
            // 填充关联数据
            fillRelatedData(List.of(vo));
        }
        return vo;
    }

    @Override
    public TableDataInfo<SysColumnMetadataVo> queryPageList(SysColumnMetadataBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysColumnMetadata> lqw = buildQueryWrapper(bo);
        Page<SysColumnMetadata> page = pageQuery.build();
        IPage<SysColumnMetadataVo> result = baseMapper.selectVoPage(page, lqw);
        
        // 填充关联数据
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            fillRelatedData(result.getRecords());
        }
        
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysColumnMetadataVo> queryList(SysColumnMetadataBo bo) {
        LambdaQueryWrapper<SysColumnMetadata> lqw = buildQueryWrapper(bo);
        List<SysColumnMetadataVo> list = baseMapper.selectVoList(lqw);
        
        // 填充关联数据
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        
        return list;
    }

    /**
     * 填充关联数据（数据源名称、创建者名称、更新者名称）
     *
     * @param list 列元数据列表
     */
    private void fillRelatedData(List<SysColumnMetadataVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        // 收集所有需要查询的ID
        Set<Long> datasourceIds = list.stream()
            .map(SysColumnMetadataVo::getDatasourceId)
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

    private LambdaQueryWrapper<SysColumnMetadata> buildQueryWrapper(SysColumnMetadataBo bo) {
        LambdaQueryWrapper<SysColumnMetadata> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getTableMetaId()), SysColumnMetadata::getTableMetaId, bo.getTableMetaId());
        lqw.eq(ObjectUtil.isNotNull(bo.getDatasourceId()), SysColumnMetadata::getDatasourceId, bo.getDatasourceId());
        lqw.like(StringUtils.isNotBlank(bo.getDatabaseName()), SysColumnMetadata::getDatabaseName, bo.getDatabaseName());
        lqw.like(StringUtils.isNotBlank(bo.getTableName()), SysColumnMetadata::getTableName, bo.getTableName());
        lqw.like(StringUtils.isNotBlank(bo.getColumnName()), SysColumnMetadata::getColumnName, bo.getColumnName());
        lqw.like(StringUtils.isNotBlank(bo.getColumnComment()), SysColumnMetadata::getColumnComment, bo.getColumnComment());
        lqw.eq(StringUtils.isNotBlank(bo.getDataType()), SysColumnMetadata::getDataType, bo.getDataType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsNullable()), SysColumnMetadata::getIsNullable, bo.getIsNullable());
        lqw.eq(StringUtils.isNotBlank(bo.getIsPrimaryKey()), SysColumnMetadata::getIsPrimaryKey, bo.getIsPrimaryKey());
        lqw.eq(StringUtils.isNotBlank(bo.getIsForeignKey()), SysColumnMetadata::getIsForeignKey, bo.getIsForeignKey());
        lqw.eq(StringUtils.isNotBlank(bo.getIsUniqueKey()), SysColumnMetadata::getIsUniqueKey, bo.getIsUniqueKey());
        lqw.eq(StringUtils.isNotBlank(bo.getIsIndexed()), SysColumnMetadata::getIsIndexed, bo.getIsIndexed());
        lqw.like(StringUtils.isNotBlank(bo.getBusinessName()), SysColumnMetadata::getBusinessName, bo.getBusinessName());
        lqw.like(StringUtils.isNotBlank(bo.getBusinessDescription()), SysColumnMetadata::getBusinessDescription, bo.getBusinessDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getDataClassification()), SysColumnMetadata::getDataClassification, bo.getDataClassification());
        lqw.eq(StringUtils.isNotBlank(bo.getSensitivityLevel()), SysColumnMetadata::getSensitivityLevel, bo.getSensitivityLevel());
        lqw.eq(StringUtils.isNotBlank(bo.getIsPii()), SysColumnMetadata::getIsPii, bo.getIsPii());
        lqw.eq(StringUtils.isNotBlank(bo.getSyncStatus()), SysColumnMetadata::getSyncStatus, bo.getSyncStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysColumnMetadata::getStatus, bo.getStatus());
        lqw.between(ObjectUtil.isAllNotEmpty(bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime")),
                SysColumnMetadata::getCreateTime, bo.getParams().get("beginCreateTime"), bo.getParams().get("endCreateTime"));
        lqw.orderByAsc(SysColumnMetadata::getOrdinalPosition);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysColumnMetadataBo bo) {
        SysColumnMetadata add = MapstructUtils.convert(bo, SysColumnMetadata.class);
        
        // 检查是否已存在相同的字段
        SysColumnMetadataVo existingColumn = baseMapper.selectByDatasourceAndTableAndColumn(
            bo.getDatasourceId(), bo.getDatabaseName(), bo.getTableName(), bo.getColumnName());
        if (ObjectUtil.isNotNull(existingColumn)) {
            throw new ServiceException("字段元数据已存在：" + bo.getDatabaseName() + "." + bo.getTableName() + "." + bo.getColumnName());
        }
        
        // 设置默认值
        if (StringUtils.isBlank(add.getSyncStatus())) {
            add.setSyncStatus("0");
        }
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("0");
        }
        if (StringUtils.isBlank(add.getIsPrimaryKey())) {
            add.setIsPrimaryKey("0");
        }
        if (StringUtils.isBlank(add.getIsForeignKey())) {
            add.setIsForeignKey("0");
        }
        if (StringUtils.isBlank(add.getIsUniqueKey())) {
            add.setIsUniqueKey("0");
        }
        if (StringUtils.isBlank(add.getIsIndexed())) {
            add.setIsIndexed("0");
        }
        if (StringUtils.isBlank(add.getIsPii())) {
            add.setIsPii("0");
        }
        if (StringUtils.isBlank(add.getSensitivityLevel())) {
            add.setSensitivityLevel("public");
        }
        if (StringUtils.isBlank(add.getIsNullable())) {
            add.setIsNullable("YES");
        }
        
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setColumnMetaId(add.getColumnMetaId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysColumnMetadataBo bo) {
        SysColumnMetadata update = MapstructUtils.convert(bo, SysColumnMetadata.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            return false;
        }
        
        // 这里可以添加删除前的验证逻辑
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public List<SysColumnMetadataVo> queryByTableMetaId(Long tableMetaId) {
        List<SysColumnMetadataVo> list = baseMapper.selectByTableMetaId(tableMetaId);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> queryByDatasourceId(Long datasourceId) {
        List<SysColumnMetadataVo> list = baseMapper.selectByDatasourceId(datasourceId);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> queryByDatasourceAndTable(Long datasourceId, String databaseName, String tableName) {
        List<SysColumnMetadataVo> list = baseMapper.selectByDatasourceAndTable(datasourceId, databaseName, tableName);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public SysColumnMetadataVo queryByDatasourceAndTableAndColumn(Long datasourceId, String databaseName, String tableName, String columnName) {
        SysColumnMetadataVo vo = baseMapper.selectByDatasourceAndTableAndColumn(datasourceId, databaseName, tableName, columnName);
        if (vo != null) {
            fillRelatedData(List.of(vo));
        }
        return vo;
    }

    @Override
    public List<SysColumnMetadataVo> queryBySyncStatus(String syncStatus) {
        List<SysColumnMetadataVo> list = baseMapper.selectBySyncStatus(syncStatus);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> queryPrimaryKeys(Long datasourceId, String databaseName, String tableName) {
        List<SysColumnMetadataVo> list = baseMapper.selectPrimaryKeys(datasourceId, databaseName, tableName);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> queryForeignKeys(Long datasourceId, String databaseName, String tableName) {
        List<SysColumnMetadataVo> list = baseMapper.selectForeignKeys(datasourceId, databaseName, tableName);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> querySensitiveColumns(Long datasourceId, String sensitivityLevel) {
        List<SysColumnMetadataVo> list = baseMapper.selectSensitiveColumns(datasourceId, sensitivityLevel);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public List<SysColumnMetadataVo> queryPiiColumns(Long datasourceId) {
        List<SysColumnMetadataVo> list = baseMapper.selectPiiColumns(datasourceId);
        if (list != null && !list.isEmpty()) {
            fillRelatedData(list);
        }
        return list;
    }

    @Override
    public long countByTableMetaId(Long tableMetaId) {
        return baseMapper.countByTableMetaId(tableMetaId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchInsert(List<SysColumnMetadataBo> columnMetadataList) {
        if (ObjectUtil.isEmpty(columnMetadataList)) {
            return false;
        }
        
        List<SysColumnMetadata> entityList = MapstructUtils.convert(columnMetadataList, SysColumnMetadata.class);
        
        // 设置默认值
        for (SysColumnMetadata entity : entityList) {
            if (StringUtils.isBlank(entity.getSyncStatus())) {
                entity.setSyncStatus("1");
            }
            if (StringUtils.isBlank(entity.getStatus())) {
                entity.setStatus("0");
            }
            if (StringUtils.isBlank(entity.getIsPrimaryKey())) {
                entity.setIsPrimaryKey("0");
            }
            if (StringUtils.isBlank(entity.getIsForeignKey())) {
                entity.setIsForeignKey("0");
            }
            if (StringUtils.isBlank(entity.getIsUniqueKey())) {
                entity.setIsUniqueKey("0");
            }
            if (StringUtils.isBlank(entity.getIsIndexed())) {
                entity.setIsIndexed("0");
            }
            if (StringUtils.isBlank(entity.getIsPii())) {
                entity.setIsPii("0");
            }
            if (StringUtils.isBlank(entity.getSensitivityLevel())) {
                entity.setSensitivityLevel("public");
            }
            if (StringUtils.isBlank(entity.getIsNullable())) {
                entity.setIsNullable("YES");
            }
        }
        
        return baseMapper.insertBatch(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean syncTableColumns(Long tableMetaId) {
        log.info("开始同步表字段结构信息，表元数据ID：{}", tableMetaId);
        
        try {
            // TODO: 这里应该实现具体的字段同步逻辑
            // 1. 获取表元数据信息
            // 2. 连接数据库获取字段结构信息
            // 3. 更新或插入字段元数据信息
            // 4. 更新同步状态和时间
            
            // 暂时只更新同步时间作为示例
            LambdaUpdateWrapper<SysColumnMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysColumnMetadata::getTableMetaId, tableMetaId)
                         .eq(SysColumnMetadata::getSyncStatus, "0")
                         .set(SysColumnMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysColumnMetadata::getSyncStatus, "1");
            
            baseMapper.update(null, updateWrapper);
            
            log.info("同步表字段结构信息完成，表元数据ID：{}", tableMetaId);
            return true;
        } catch (Exception e) {
            log.error("同步表字段结构信息失败，表元数据ID：{}，错误信息：{}", tableMetaId, e.getMessage(), e);
            
            // 更新同步状态为失败
            LambdaUpdateWrapper<SysColumnMetadata> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(SysColumnMetadata::getTableMetaId, tableMetaId)
                         .eq(SysColumnMetadata::getSyncStatus, "0")
                         .set(SysColumnMetadata::getLastSyncTime, LocalDateTime.now())
                         .set(SysColumnMetadata::getSyncStatus, "2");
            
            baseMapper.update(null, updateWrapper);
            
            throw new ServiceException("同步字段结构信息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSyncStatus(Long columnMetaId, String syncStatus, String syncErrorMessage) {
        LambdaUpdateWrapper<SysColumnMetadata> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysColumnMetadata::getColumnMetaId, columnMetaId)
                     .set(SysColumnMetadata::getLastSyncTime, LocalDateTime.now())
                     .set(SysColumnMetadata::getSyncStatus, syncStatus);
        
        return baseMapper.update(null, updateWrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBusinessInfo(Long columnMetaId, String businessName, String businessDescription,
                                      String dataClassification, String sensitivityLevel, String isPii,
                                      String maskingRule, String validationRule) {
        LambdaUpdateWrapper<SysColumnMetadata> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysColumnMetadata::getColumnMetaId, columnMetaId);
        
        if (StringUtils.isNotBlank(businessName)) {
            updateWrapper.set(SysColumnMetadata::getBusinessName, businessName);
        }
        if (StringUtils.isNotBlank(businessDescription)) {
            updateWrapper.set(SysColumnMetadata::getBusinessDescription, businessDescription);
        }
        if (StringUtils.isNotBlank(dataClassification)) {
            updateWrapper.set(SysColumnMetadata::getDataClassification, dataClassification);
        }
        if (StringUtils.isNotBlank(sensitivityLevel)) {
            updateWrapper.set(SysColumnMetadata::getSensitivityLevel, sensitivityLevel);
        }
        if (StringUtils.isNotBlank(isPii)) {
            updateWrapper.set(SysColumnMetadata::getIsPii, isPii);
        }
        if (StringUtils.isNotBlank(maskingRule)) {
            updateWrapper.set(SysColumnMetadata::getMaskingRule, maskingRule);
        }
        if (StringUtils.isNotBlank(validationRule)) {
            updateWrapper.set(SysColumnMetadata::getValidationRule, validationRule);
        }
        
        return baseMapper.update(null, updateWrapper) > 0;
    }

}