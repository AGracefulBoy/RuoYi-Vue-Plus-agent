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
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.bo.SysDatasourceBo;
import org.dromara.system.domain.vo.SysDatasourceListVo;
import org.dromara.system.domain.vo.SysDatasourceVo;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.service.ISysDatasourceService;
import org.dromara.system.service.ISysTableMetadataService;
import org.dromara.system.service.ISysColumnMetadataService;
import org.dromara.system.domain.bo.SysTableMetadataBo;
import org.dromara.system.domain.bo.SysColumnMetadataBo;
import org.dromara.system.util.DatasourceValidatorUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

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
    private final ISysTableMetadataService tableMetadataService;
    private final ISysColumnMetadataService columnMetadataService;

    @Override
    public SysDatasourceVo queryById(Long datasourceId) {
        return baseMapper.selectVoById(datasourceId);
    }

    @Override
    public TableDataInfo<SysDatasourceVo> queryPageList(SysDatasourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        Page<SysDatasource> page = pageQuery.build();
        IPage<SysDatasourceVo> result = baseMapper.selectDatasourceListVoPage(page, lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<SysDatasourceVo> queryList(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        return baseMapper.selectDatasourceListVo(lqw);
    }

    @Override
    public TableDataInfo<SysDatasourceListVo> queryPageListForList(SysDatasourceBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDatasource> lqw = buildQueryWrapper(bo);
        Page<SysDatasource> page = pageQuery.build();
        IPage<SysDatasourceListVo> result = baseMapper.selectDatasourceListVoPageForList(page, lqw);
        return TableDataInfo.build(result);
    }

    private LambdaQueryWrapper<SysDatasource> buildQueryWrapper(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getDatasourceName()), SysDatasource::getDatasourceName, bo.getDatasourceName());
        lqw.eq(StringUtils.isNotBlank(bo.getDatasourceType()), SysDatasource::getDatasourceType, bo.getDatasourceType());
        lqw.eq(StringUtils.isNotBlank(bo.getDatabaseType()), SysDatasource::getDatabaseType, bo.getDatabaseType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysDatasource::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getIsDefault()), SysDatasource::getIsDefault, bo.getIsDefault());
        lqw.eq(StringUtils.isNotBlank(bo.getConnectionStatus()), SysDatasource::getConnectionStatus, bo.getConnectionStatus());
        // 注意：del_flag 的过滤已经在 Mapper 的 SQL 中处理，这里不再添加
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

            // If datasource is MySQL database type, asynchronously fetch metadata
            if ("database".equals(add.getDatasourceType()) && "mysql".equalsIgnoreCase(add.getDatabaseType())) {
                asyncFetchMysqlMetadata(add);
            }
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
            try {
                bo.setPassword(EncryptUtils.decryptByAes(bo.getPassword(), ""));
            } catch (Exception e) {
                log.warn("密码解密失败，使用原始密码: {}", e.getMessage());
                // 保持原始密码不变
            }
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
        // 过滤已删除的数据
        lqw.eq(SysDatasource::getDelFlag, SystemConstants.NORMAL);
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

    /**
     * 异步获取MySQL数据库元数据，包括表和列信息。
     *
     * @param datasource 数据源配置
     */
    @Async
    public void asyncFetchMysqlMetadata(SysDatasource datasource) {
        log.info("开始为MySQL数据源异步获取元数据: {}", datasource.getDatasourceName());

        try {
            // 解密连接密码
            String decryptedPassword = datasource.getPassword();
            if (StringUtils.isNotBlank(decryptedPassword)) {
                try {
                    // 尝试解密密码，如果解密失败则使用原密码
                    decryptedPassword = EncryptUtils.decryptByAes(decryptedPassword, "");
                } catch (Exception e) {
                    log.warn("密码解密失败，使用原始密码: {}", e.getMessage());
                    // 保持原始密码
                }
            }

            // 构建连接配置
            SysDatasourceBo connectionConfig = MapstructUtils.convert(datasource, SysDatasourceBo.class);
            connectionConfig.setPassword(decryptedPassword);

            // 从MySQL数据库获取元数据
            fetchMysqlDatabaseMetadata(connectionConfig);

            log.info("成功完成数据源的异步元数据获取: {}", datasource.getDatasourceName());
        } catch (Exception exception) {
            log.error("获取数据源元数据失败: {}, 错误: {}",
                datasource.getDatasourceName(), exception.getMessage(), exception);
        }
    }

    /**
     * 获取MySQL数据库元数据并保存到元数据表中。
     *
     * @param datasourceConfig 带有解密密码的数据源配置
     */
    private void fetchMysqlDatabaseMetadata(SysDatasourceBo datasourceConfig) {
        String connectionUrl = buildConnectionUrl(datasourceConfig);

        // 加载MySQL驱动
        try {
            if (StringUtils.isNotBlank(datasourceConfig.getDriverClassName())) {
                Class.forName(datasourceConfig.getDriverClassName());
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            log.error("MySQL驱动加载失败: {}", e.getMessage(), e);
            throw new ServiceException("MySQL驱动加载失败: " + e.getMessage());
        }

        Connection connection = null;
        try {
            // 创建连接
            connection = DriverManager.getConnection(
                connectionUrl,
                datasourceConfig.getUsername(),
                datasourceConfig.getPassword());

            // 设置连接为只读模式，提高性能
            connection.setReadOnly(true);

            DatabaseMetaData databaseMetaData = connection.getMetaData();
            String databaseName = datasourceConfig.getDatabaseName();

            // 获取表元数据
            List<SysTableMetadataBo> tableMetadataList = fetchTableMetadata(
                connection, datasourceConfig.getDatasourceId(), databaseName);

            // 保存表元数据并为每个表获取列元数据
            for (SysTableMetadataBo tableMetadata : tableMetadataList) {
                try {
                    // 保存表元数据
                    if (tableMetadataService.insertByBo(tableMetadata)) {
                        // 获取列元数据，传递connection而不是databaseMetaData
                        List<SysColumnMetadataBo> columnMetadataList = fetchColumnMetadataWithConnection(
                            connection,
                            tableMetadata.getTableMetaId(),
                            datasourceConfig.getDatasourceId(),
                            databaseName,
                            tableMetadata.getTableName());

                        // 批量插入列元数据
                        if (!columnMetadataList.isEmpty()) {
                            columnMetadataService.batchInsert(columnMetadataList);
                        }

                        log.debug("为表获取了{}列: {}.{}",
                            columnMetadataList.size(), databaseName, tableMetadata.getTableName());
                    }
                } catch (Exception exception) {
                    log.error("获取表的列元数据失败: {}.{}, 错误: {}",
                        databaseName, tableMetadata.getTableName(), exception.getMessage());
                }
            }

        } catch (SQLException exception) {
            log.error("连接MySQL数据库失败: {}, 错误: {}",
                datasourceConfig.getDatabaseName(), exception.getMessage(), exception);
            throw new ServiceException("获取数据库元数据失败: " + exception.getMessage());
        } finally {
            // 确保连接被正确关闭
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.warn("关闭数据库连接时出错: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 从MySQL数据库获取表元数据。
     *
     * @param connection 数据库连接
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @return 表元数据列表
     */
    private List<SysTableMetadataBo> fetchTableMetadata(Connection connection,
            Long datasourceId, String databaseName) {
        List<SysTableMetadataBo> tableMetadataList = new ArrayList<>();

        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            try (ResultSet tablesResultSet = databaseMetaData.getTables(
                    databaseName, null, null, new String[]{"TABLE", "VIEW"})) {

                while (tablesResultSet.next()) {
                    SysTableMetadataBo tableMetadata = new SysTableMetadataBo();
                    tableMetadata.setDatasourceId(datasourceId);
                    tableMetadata.setDatabaseName(databaseName);
                    tableMetadata.setTableName(tablesResultSet.getString("TABLE_NAME"));
                    tableMetadata.setTableComment(tablesResultSet.getString("REMARKS"));
                    tableMetadata.setTableType(tablesResultSet.getString("TABLE_TYPE"));
                    tableMetadata.setSyncStatus("1");
                    tableMetadata.setLastSyncTime(LocalDateTime.now());
                    tableMetadata.setStatus("0");

                    // Fetch additional table information using information_schema
                    fetchAdditionalTableInfo(connection, tableMetadata);

                    tableMetadataList.add(tableMetadata);
                }
            }
        } catch (SQLException exception) {
            log.error("Failed to fetch table metadata for database: {}, error: {}",
                databaseName, exception.getMessage(), exception);
        }

        return tableMetadataList;
    }

    /**
     * Fetch additional table information from information_schema.
     *
     * @param connection the database connection
     * @param tableMetadata the table metadata to enrich
     */
    private void fetchAdditionalTableInfo(Connection connection, SysTableMetadataBo tableMetadata) {
        String query = """
            SELECT
                ENGINE,
                TABLE_COLLATION,
                TABLE_ROWS,
                DATA_LENGTH,
                INDEX_LENGTH,
                CREATE_TIME,
                UPDATE_TIME
            FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, tableMetadata.getDatabaseName());
            statement.setString(2, tableMetadata.getTableName());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    tableMetadata.setEngine(resultSet.getString("ENGINE"));
                    tableMetadata.setCollation(resultSet.getString("TABLE_COLLATION"));
                    tableMetadata.setTableRows(resultSet.getLong("TABLE_ROWS"));
                    tableMetadata.setDataLength(resultSet.getLong("DATA_LENGTH"));
                    tableMetadata.setIndexLength(resultSet.getLong("INDEX_LENGTH"));

                    Timestamp createTime = resultSet.getTimestamp("CREATE_TIME");
                    if (createTime != null) {
                        tableMetadata.setCreateTimeDb(createTime.toLocalDateTime());
                    }

                    Timestamp updateTime = resultSet.getTimestamp("UPDATE_TIME");
                    if (updateTime != null) {
                        tableMetadata.setUpdateTimeDb(updateTime.toLocalDateTime());
                    }
                }
            }
        } catch (SQLException exception) {
            log.warn("Failed to fetch additional table info for: {}.{}, error: {}",
                tableMetadata.getDatabaseName(), tableMetadata.getTableName(), exception.getMessage());
        }
    }

    /**
     * 从MySQL数据库为指定表获取列元数据。
     *
     * @param connection 数据库连接
     * @param tableMetaId 表元数据ID
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return 列元数据列表
     */
    private List<SysColumnMetadataBo> fetchColumnMetadataWithConnection(Connection connection,
            Long tableMetaId, Long datasourceId, String databaseName, String tableName) {
        List<SysColumnMetadataBo> columnMetadataList = new ArrayList<>();

        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            try (ResultSet columnsResultSet = databaseMetaData.getColumns(databaseName, null, tableName, null)) {

                while (columnsResultSet.next()) {
                    SysColumnMetadataBo columnMetadata = new SysColumnMetadataBo();
                    columnMetadata.setTableMetaId(tableMetaId);
                    columnMetadata.setDatasourceId(datasourceId);
                    columnMetadata.setDatabaseName(databaseName);
                    columnMetadata.setTableName(tableName);
                    columnMetadata.setColumnName(columnsResultSet.getString("COLUMN_NAME"));
                    columnMetadata.setColumnComment(columnsResultSet.getString("REMARKS"));
                    columnMetadata.setOrdinalPosition(columnsResultSet.getInt("ORDINAL_POSITION"));
                    columnMetadata.setColumnDefault(columnsResultSet.getString("COLUMN_DEF"));
                    columnMetadata.setIsNullable(columnsResultSet.getString("IS_NULLABLE"));
                    columnMetadata.setDataType(columnsResultSet.getString("TYPE_NAME"));
                    columnMetadata.setColumnType(columnsResultSet.getString("TYPE_NAME"));

                    // Handle character and numeric type information
                    int columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                    int decimalDigits = columnsResultSet.getInt("DECIMAL_DIGITS");

                    if (isCharacterType(columnMetadata.getDataType())) {
                        columnMetadata.setCharacterMaximumLength(columnSize);
                    } else if (isNumericType(columnMetadata.getDataType())) {
                        columnMetadata.setNumericPrecision(columnSize);
                        columnMetadata.setNumericScale(decimalDigits);
                    }

                    columnMetadata.setSyncStatus("1");
                    columnMetadata.setLastSyncTime(LocalDateTime.now());
                    columnMetadata.setStatus("0");

                    // Check if column is primary key
                    if (isPrimaryKeyWithConnection(connection, databaseName, tableName, columnMetadata.getColumnName())) {
                        columnMetadata.setIsPrimaryKey("1");
                        columnMetadata.setColumnKey("PRI");
                    } else {
                        columnMetadata.setIsPrimaryKey("0");
                    }

                    columnMetadataList.add(columnMetadata);
                }

            }
        } catch (SQLException exception) {
            log.error("Failed to fetch column metadata for table: {}.{}, error: {}",
                databaseName, tableName, exception.getMessage(), exception);
        }

        return columnMetadataList;
    }

    /**
     * 从MySQL数据库为指定表获取列元数据（保留原方法以兼容其他调用）。
     *
     * @param databaseMetaData 数据库元数据对象
     * @param tableMetaId 表元数据ID
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName 表名称
     * @return 列元数据列表
     */
    private List<SysColumnMetadataBo> fetchColumnMetadata(DatabaseMetaData databaseMetaData,
            Long tableMetaId, Long datasourceId, String databaseName, String tableName) {
        List<SysColumnMetadataBo> columnMetadataList = new ArrayList<>();

        try (ResultSet columnsResultSet = databaseMetaData.getColumns(databaseName, null, tableName, null)) {

            while (columnsResultSet.next()) {
                SysColumnMetadataBo columnMetadata = new SysColumnMetadataBo();
                columnMetadata.setTableMetaId(tableMetaId);
                columnMetadata.setDatasourceId(datasourceId);
                columnMetadata.setDatabaseName(databaseName);
                columnMetadata.setTableName(tableName);
                columnMetadata.setColumnName(columnsResultSet.getString("COLUMN_NAME"));
                columnMetadata.setColumnComment(columnsResultSet.getString("REMARKS"));
                columnMetadata.setOrdinalPosition(columnsResultSet.getInt("ORDINAL_POSITION"));
                columnMetadata.setColumnDefault(columnsResultSet.getString("COLUMN_DEF"));
                columnMetadata.setIsNullable(columnsResultSet.getString("IS_NULLABLE"));
                columnMetadata.setDataType(columnsResultSet.getString("TYPE_NAME"));
                columnMetadata.setColumnType(columnsResultSet.getString("TYPE_NAME"));

                // Handle character and numeric type information
                int columnSize = columnsResultSet.getInt("COLUMN_SIZE");
                int decimalDigits = columnsResultSet.getInt("DECIMAL_DIGITS");

                if (isCharacterType(columnMetadata.getDataType())) {
                    columnMetadata.setCharacterMaximumLength(columnSize);
                } else if (isNumericType(columnMetadata.getDataType())) {
                    columnMetadata.setNumericPrecision(columnSize);
                    columnMetadata.setNumericScale(decimalDigits);
                }

                columnMetadata.setSyncStatus("1");
                columnMetadata.setLastSyncTime(LocalDateTime.now());
                columnMetadata.setStatus("0");

                // Check if column is primary key
                if (isPrimaryKey(databaseMetaData, databaseName, tableName, columnMetadata.getColumnName())) {
                    columnMetadata.setIsPrimaryKey("1");
                    columnMetadata.setColumnKey("PRI");
                } else {
                    columnMetadata.setIsPrimaryKey("0");
                }

                columnMetadataList.add(columnMetadata);
            }

        } catch (SQLException exception) {
            log.error("Failed to fetch column metadata for table: {}.{}, error: {}",
                databaseName, tableName, exception.getMessage(), exception);
        }

        return columnMetadataList;
    }

    /**
     * Check if a column is a primary key using connection.
     *
     * @param connection the database connection
     * @param databaseName the database name
     * @param tableName the table name
     * @param columnName the column name
     * @return true if the column is a primary key
     */
    private boolean isPrimaryKeyWithConnection(Connection connection, String databaseName,
            String tableName, String columnName) {
        try {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            try (ResultSet primaryKeysResultSet = databaseMetaData.getPrimaryKeys(databaseName, null, tableName)) {
                while (primaryKeysResultSet.next()) {
                    if (columnName.equals(primaryKeysResultSet.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
            }
        } catch (SQLException exception) {
            log.warn("Failed to check primary key for column: {}.{}.{}, error: {}",
                databaseName, tableName, columnName, exception.getMessage());
        }
        return false;
    }

    /**
     * Check if a column is a primary key.
     *
     * @param databaseMetaData the database metadata object
     * @param databaseName the database name
     * @param tableName the table name
     * @param columnName the column name
     * @return true if the column is a primary key
     */
    private boolean isPrimaryKey(DatabaseMetaData databaseMetaData, String databaseName,
            String tableName, String columnName) {
        try (ResultSet primaryKeysResultSet = databaseMetaData.getPrimaryKeys(databaseName, null, tableName)) {
            while (primaryKeysResultSet.next()) {
                if (columnName.equals(primaryKeysResultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        } catch (SQLException exception) {
            log.warn("Failed to check primary key for column: {}.{}.{}, error: {}",
                databaseName, tableName, columnName, exception.getMessage());
        }
        return false;
    }

    /**
     * Check if a data type is a character type.
     *
     * @param dataType the data type
     * @return true if it's a character type
     */
    private boolean isCharacterType(String dataType) {
        if (dataType == null) return false;
        String lowerType = dataType.toLowerCase();
        return lowerType.contains("char") || lowerType.contains("text") || lowerType.contains("blob");
    }

    /**
     * Check if a data type is a numeric type.
     *
     * @param dataType the data type
     * @return true if it's a numeric type
     */
    private boolean isNumericType(String dataType) {
        if (dataType == null) return false;
        String lowerType = dataType.toLowerCase();
        return lowerType.contains("int") || lowerType.contains("decimal") ||
               lowerType.contains("numeric") || lowerType.contains("float") ||
               lowerType.contains("double");
    }

}
