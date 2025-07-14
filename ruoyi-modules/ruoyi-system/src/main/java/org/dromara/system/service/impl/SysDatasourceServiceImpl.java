package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.bo.SysDatasourceBo;
import org.dromara.system.domain.vo.SysDatasourceVo;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.service.ISysDatasourceService;
import org.dromara.system.util.DatasourceValidatorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 数据源管理Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysDatasourceServiceImpl implements ISysDatasourceService {

    private final SysDatasourceMapper baseMapper;

    @Override
    public SysDatasourceVo queryById(Long datasourceId) {
        return baseMapper.selectVoById(datasourceId);
    }

    @Override
    public TableDataInfo<SysDatasourceVo> queryPageList(SysDatasourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        Page<SysDatasourceVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysDatasourceVo> queryList(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysDatasource> buildQueryWrapper(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDatasourceName()), SysDatasource::getDatasourceName, bo.getDatasourceName());
        lqw.eq(StringUtils.isNotBlank(bo.getDatasourceType()), SysDatasource::getDatasourceType, bo.getDatasourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getDatabaseType()), SysDatasource::getDatabaseType, bo.getDatabaseType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysDatasource::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getIsDefault()), SysDatasource::getIsDefault, bo.getIsDefault());
        lqw.eq(StringUtils.isNotBlank(bo.getConnectionStatus()), SysDatasource::getConnectionStatus, bo.getConnectionStatus());
        lqw.orderByDesc(SysDatasource::getCreateTime);
        return lqw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysDatasourceBo bo) {
        // 校验数据源名称唯一性
        if (!checkDatasourceNameUnique(bo)) {
            throw new ServiceException("数据源名称已存在");
        }

        // 校验数据源类型相关字段
        String validateResult = validateDatasourceFields(bo);
        if (StringUtils.isNotBlank(validateResult)) {
            throw new ServiceException(validateResult);
        }

        SysDatasource add = MapstructUtils.convert(bo, SysDatasource.class);

        // 设置默认值
        setDefaultValues(add);

        // 加密密码
        if (StringUtils.isNotBlank(add.getPassword())) {

            add.setPassword(add.getPassword().replaceAll("\\s", ""));
        }

        // 如果设置为默认数据源，需要先取消其他默认数据源
        if ("1".equals(add.getIsDefault())) {
            clearOtherDefaultDatasource();
        }

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setDatasourceId(add.getDatasourceId());
        }
        return flag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysDatasourceBo bo) {
        // 校验数据源名称唯一性
        if (!checkDatasourceNameUnique(bo)) {
            throw new ServiceException("数据源名称已存在");
        }

        // 校验数据源类型相关字段
        String validateResult = validateDatasourceFields(bo);
        if (StringUtils.isNotBlank(validateResult)) {
            throw new ServiceException(validateResult);
        }

        SysDatasource update = MapstructUtils.convert(bo, SysDatasource.class);

        // 如果密码有变更，需要重新加密
        if (StringUtils.isNotBlank(update.getPassword())) {
            // 判断是否是新密码（简单判断长度，实际项目中可能需要更复杂的判断）
            SysDatasource existing = baseMapper.selectById(update.getDatasourceId());
            if (existing != null && !update.getPassword().equals(existing.getPassword())) {
                update.setPassword(update.getPassword().replaceAll("\\s", ""));
            }
        }

        // 如果设置为默认数据源，需要先取消其他默认数据源
        if ("1".equals(update.getIsDefault())) {
            clearOtherDefaultDatasource(update.getDatasourceId());
        }

        return baseMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否存在关联数据，这里可以根据实际业务需求添加校验逻辑
            for (Long id : ids) {
                SysDatasource datasource = baseMapper.selectById(id);
                if (ObjectUtil.isNotNull(datasource) && "1".equals(datasource.getIsDefault())) {
                    throw new ServiceException("默认数据源不能删除");
                }
            }
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean testConnection(Long datasourceId) {
        SysDatasource datasource = baseMapper.selectById(datasourceId);
        if (ObjectUtil.isNull(datasource)) {
            throw new ServiceException("数据源不存在");
        }

        // 转换为Bo进行测试
        SysDatasourceBo bo = MapstructUtils.convert(datasource, SysDatasourceBo.class);

        // 解密密码
        if (StringUtils.isNotBlank(bo.getPassword())) {
            bo.setPassword(EncryptUtils.decryptByAes(bo.getPassword(),""));
        }

        boolean isConnected = testConnectionByConfig(bo);

        // 更新连接状态
        LambdaUpdateWrapper<SysDatasource> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SysDatasource::getDatasourceId, datasourceId)
            .set(SysDatasource::getConnectionStatus, isConnected ? "1" : "2")
            .set(SysDatasource::getLastTestTime, LocalDateTime.now())
            .set(SysDatasource::getErrorMessage, isConnected ? null : "连接测试失败");

        baseMapper.update(null, updateWrapper);
        return isConnected;
    }

    @Override
    public Boolean testConnectionByConfig(SysDatasourceBo bo) {
        try {
            if ("database".equals(bo.getDatasourceType())) {
                return testDatabaseConnection(bo);
            } else if ("excel".equals(bo.getDatasourceType())) {
                return testExcelConnection(bo);
            }
            return false;
        } catch (Exception e) {
            log.error("数据源连接测试失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试数据库连接
     */
    private Boolean testDatabaseConnection(SysDatasourceBo bo) {
        String url = buildConnectionUrl(bo);
        String username = bo.getUsername();
        String password = bo.getPassword();

        try {
            // 加载驱动类
            if (StringUtils.isNotBlank(bo.getDriverClassName())) {
                Class.forName(bo.getDriverClassName());
            }

            // 设置连接超时
            int timeout = bo.getConnectionTimeout() != null ? bo.getConnectionTimeout() / 1000 : 30;
            DriverManager.setLoginTimeout(timeout);

            // 测试连接
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                // 执行测试SQL
                String testQuery = StringUtils.isNotBlank(bo.getTestQuery()) ? bo.getTestQuery() : "SELECT 1";
                connection.createStatement().execute(testQuery);
                return true;
            }
        } catch (ClassNotFoundException e) {
            log.error("数据库驱动类未找到: {}", bo.getDriverClassName(), e);
            return false;
        } catch (SQLException e) {
            log.error("数据库连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试Excel文件连接
     */
    private Boolean testExcelConnection(SysDatasourceBo bo) {
        try {
            String filePath = bo.getFilePath();
            if (StringUtils.isBlank(filePath)) {
                return false;
            }

            File file = new File(filePath);
            return file.exists() && file.isFile() && file.canRead();
        } catch (Exception e) {
            log.error("Excel文件检查失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建数据库连接URL
     */
    private String buildConnectionUrl(SysDatasourceBo bo) {
        try {
            String url = DatasourceValidatorUtils.buildConnectionUrl(bo);

            // 添加连接参数
            if (StringUtils.isNotBlank(bo.getConnectionParams())) {
                url += (url.contains("?") ? "&" : "?") + bo.getConnectionParams();
            }

            return url;
        } catch (IllegalArgumentException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public SysDatasourceVo queryByDatasourceName(String datasourceName) {
        return baseMapper.selectByDatasourceName(datasourceName);
    }

    @Override
    public List<SysDatasourceVo> queryByDatasourceType(String datasourceType) {
        return baseMapper.selectByDatasourceType(datasourceType);
    }

    @Override
    public SysDatasourceVo queryDefaultDatasource() {
        return baseMapper.selectDefaultDatasource();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultDatasource(Long datasourceId) {
        // 先取消其他默认数据源
        clearOtherDefaultDatasource(datasourceId);

        // 设置指定数据源为默认
        LambdaUpdateWrapper<SysDatasource> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SysDatasource::getDatasourceId, datasourceId)
            .set(SysDatasource::getIsDefault, "1");

        return baseMapper.update(null, updateWrapper) > 0;
    }

    @Override
    public Boolean checkDatasourceNameUnique(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysDatasource::getDatasourceName, bo.getDatasourceName());
        if (ObjectUtil.isNotNull(bo.getDatasourceId())) {
            lqw.ne(SysDatasource::getDatasourceId, bo.getDatasourceId());
        }
        return baseMapper.selectCount(lqw) == 0;
    }

    @Override
    public String validateDatasourceFields(SysDatasourceBo bo) {
        return DatasourceValidatorUtils.validateDatasource(bo);
    }

    @Override
    public List<SysDatasourceVo> queryByDatabaseType(String databaseType) {
        return baseMapper.selectByDatabaseType(databaseType);
    }

    @Override
    public List<SysDatasourceVo> queryConnectedDatasources() {
        return baseMapper.selectConnectedDatasources();
    }

    @Override
    public long countByDatasourceType(String datasourceType) {
        return baseMapper.countByDatasourceType(datasourceType);
    }

    /**
     * 设置默认值
     */
    private void setDefaultValues(SysDatasource datasource) {
        if (ObjectUtil.isNull(datasource.getMaxConnections())) {
            datasource.setMaxConnections(10);
        }
        if (ObjectUtil.isNull(datasource.getConnectionTimeout())) {
            datasource.setConnectionTimeout(30000);
        }
        if (ObjectUtil.isNull(datasource.getQueryTimeout())) {
            datasource.setQueryTimeout(60000);
        }
        if (StringUtils.isBlank(datasource.getTestQuery())) {
            datasource.setTestQuery("SELECT 1");
        }
        if (StringUtils.isBlank(datasource.getStatus())) {
            datasource.setStatus("0");
        }
        if (StringUtils.isBlank(datasource.getIsDefault())) {
            datasource.setIsDefault("0");
        }
        if (StringUtils.isBlank(datasource.getConnectionStatus())) {
            datasource.setConnectionStatus("0");
        }
    }

    /**
     * 清除其他默认数据源
     */
    private void clearOtherDefaultDatasource() {
        clearOtherDefaultDatasource(null);
    }

    /**
     * 清除其他默认数据源
     */
    private void clearOtherDefaultDatasource(Long excludeId) {
        LambdaUpdateWrapper<SysDatasource> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SysDatasource::getIsDefault, "1")
            .set(SysDatasource::getIsDefault, "0");
        if (ObjectUtil.isNotNull(excludeId)) {
            updateWrapper.ne(SysDatasource::getDatasourceId, excludeId);
        }
        baseMapper.update(null, updateWrapper);
    }

}
