package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.vo.SysDatasourceListVo;
import org.dromara.system.domain.vo.SysDatasourceVo;

import java.util.List;

/**
 * 数据源管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysDatasourceMapper extends BaseMapperPlus<SysDatasource, SysDatasourceVo> {

    /**
     * 查询数据源管理列表（包含创建者和更新者名称）
     *
     * @param wrapper 查询条件
     * @return 数据源管理列表
     */
    @Select("SELECT sd.datasource_id, sd.tenant_id, sd.datasource_name, sd.datasource_type, " +
            "sd.database_type, sd.host, sd.port, sd.database_name, sd.username, sd.password, " +
            "sd.connection_url, sd.driver_class_name, sd.file_path, sd.file_size, sd.sheet_names, " +
            "sd.connection_params, sd.max_connections, sd.connection_timeout, sd.query_timeout, " +
            "sd.test_query, sd.description, sd.status, sd.is_default, sd.connection_status, " +
            "sd.last_test_time, sd.error_message, sd.remark, sd.create_by, " +
            "u1.nick_name AS createByName, sd.create_time, sd.update_by, " +
            "u2.nick_name AS updateByName, sd.update_time " +
            "FROM sys_datasource sd " +
            "LEFT JOIN sys_user u1 ON sd.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sd.update_by = u2.user_id " +
            "WHERE sd.del_flag = '0' ${ew.customSqlSegment}")
    List<SysDatasourceVo> selectDatasourceListVo(@Param(Constants.WRAPPER) Wrapper<SysDatasource> wrapper);

    /**
     * 分页查询数据源管理列表（包含创建者和更新者名称）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 数据源管理列表
     */
    @Select("SELECT sd.datasource_id, sd.tenant_id, sd.datasource_name, sd.datasource_type, " +
            "sd.database_type, sd.host, sd.port, sd.database_name, sd.username, sd.password, " +
            "sd.connection_url, sd.driver_class_name, sd.file_path, sd.file_size, sd.sheet_names, " +
            "sd.connection_params, sd.max_connections, sd.connection_timeout, sd.query_timeout, " +
            "sd.test_query, sd.description, sd.status, sd.is_default, sd.connection_status, " +
            "sd.last_test_time, sd.error_message, sd.remark, sd.create_by, " +
            "u1.nick_name AS createByName, sd.create_time, sd.update_by, " +
            "u2.nick_name AS updateByName, sd.update_time " +
            "FROM sys_datasource sd " +
            "LEFT JOIN sys_user u1 ON sd.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sd.update_by = u2.user_id " +
            "WHERE sd.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysDatasourceVo> selectDatasourceListVoPage(IPage<SysDatasource> page, @Param(Constants.WRAPPER) Wrapper<SysDatasource> wrapper);

    /**
     * 分页查询数据源管理列表（精简版）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 数据源管理列表（只包含核心字段）
     */
    @Select("SELECT sd.datasource_id, sd.datasource_name, sd.datasource_type, sd.database_type," +
            "u1.nick_name AS createByName, sd.create_time, sd.update_time, sd.status , sd.sync_status " +
            "FROM sys_datasource sd " +
            "LEFT JOIN sys_user u1 ON sd.create_by = u1.user_id " +
            "WHERE sd.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysDatasourceListVo> selectDatasourceListVoPageForList(IPage<SysDatasource> page, @Param(Constants.WRAPPER) Wrapper<SysDatasource> wrapper);

    /**
     * 根据数据源名称查询数据源
     *
     * @param datasourceName 数据源名称
     * @return 数据源信息
     */
    default SysDatasourceVo selectByDatasourceName(String datasourceName) {
        return selectVoOne(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getDatasourceName, datasourceName)
                .eq(SysDatasource::getDelFlag, SystemConstants.NORMAL)
        );
    }

    /**
     * 根据数据源类型查询数据源列表
     *
     * @param datasourceType 数据源类型
     * @return 数据源列表
     */
    default List<SysDatasourceVo> selectByDatasourceType(String datasourceType) {
        return selectDatasourceListVo(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getDatasourceType, datasourceType)
                .eq(SysDatasource::getStatus, "0")
                .orderByDesc(SysDatasource::getCreateTime)
        );
    }

    /**
     * 查询默认数据源
     *
     * @return 默认数据源
     */
    default SysDatasourceVo selectDefaultDatasource() {
        return selectVoOne(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getIsDefault, "1")
                .eq(SysDatasource::getStatus, "0")
        );
    }

    /**
     * 查询可用的数据库类型数据源
     *
     * @param databaseType 数据库类型
     * @return 数据源列表
     */
    default List<SysDatasourceVo> selectByDatabaseType(String databaseType) {
        return selectDatasourceListVo(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getDatasourceType, "database")
                .eq(SysDatasource::getDatabaseType, databaseType)
                .eq(SysDatasource::getStatus, "0")
                .orderByDesc(SysDatasource::getCreateTime)
        );
    }

    /**
     * 查询连接成功的数据源
     *
     * @return 数据源列表
     */
    default List<SysDatasourceVo> selectConnectedDatasources() {
        return selectDatasourceListVo(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getConnectionStatus, "1")
                .eq(SysDatasource::getStatus, "0")
                .orderByDesc(SysDatasource::getLastTestTime)
        );
    }

    /**
     * 统计不同类型数据源数量
     *
     * @param datasourceType 数据源类型
     * @return 数量
     */
    default long countByDatasourceType(String datasourceType) {
        return selectCount(
            new LambdaQueryWrapper<SysDatasource>()
                .eq(SysDatasource::getDatasourceType, datasourceType)
                .eq(SysDatasource::getStatus, "0")
        );
    }

}
