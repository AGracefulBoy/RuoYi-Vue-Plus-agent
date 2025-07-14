package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.vo.SysDatasourceVo;

import java.util.List;

/**
 * 数据源管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysDatasourceMapper extends BaseMapperPlus<SysDatasource, SysDatasourceVo> {

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
        );
    }

    /**
     * 根据数据源类型查询数据源列表
     *
     * @param datasourceType 数据源类型
     * @return 数据源列表
     */
    default List<SysDatasourceVo> selectByDatasourceType(String datasourceType) {
        return selectVoList(
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
        return selectVoList(
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
        return selectVoList(
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