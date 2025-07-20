package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysDatasourceBo;
import org.dromara.system.domain.vo.SysDatasourceListVo;
import org.dromara.system.domain.vo.SysDatasourceVo;

import java.util.Collection;
import java.util.List;

/**
 * 数据源管理Service接口
 *
 * @author ruoyi
 */
public interface ISysDatasourceService {

    /**
     * 查询数据源管理
     *
     * @param datasourceId 数据源ID
     * @return 数据源信息
     */
    SysDatasourceVo queryById(Long datasourceId);

    /**
     * 查询数据源管理列表（分页）
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 数据源列表
     */
    TableDataInfo<SysDatasourceVo> queryPageList(SysDatasourceBo bo, PageQuery pageQuery);

    /**
     * 查询数据源管理列表（分页）- 精简版
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 数据源列表（只包含核心字段）
     */
    TableDataInfo<SysDatasourceListVo> queryPageListForList(SysDatasourceBo bo, PageQuery pageQuery);

    /**
     * 查询数据源管理列表
     *
     * @param bo 查询条件
     * @return 数据源列表
     */
    List<SysDatasourceVo> queryList(SysDatasourceBo bo);

    /**
     * 新增数据源管理
     *
     * @param bo 数据源信息
     * @return 结果
     */
    Boolean insertByBo(SysDatasourceBo bo);

    /**
     * 修改数据源管理
     *
     * @param bo 数据源信息
     * @return 结果
     */
    Boolean updateByBo(SysDatasourceBo bo);

    /**
     * 校验并批量删除数据源管理信息
     *
     * @param ids     需要删除的数据源ID
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 测试数据源连接
     *
     * @param datasourceId 数据源ID
     * @return 测试结果
     */
    Boolean testConnection(Long datasourceId);

    /**
     * 测试数据源连接（使用传入的配置）
     *
     * @param bo 数据源配置
     * @return 测试结果
     */
    Boolean testConnectionByConfig(SysDatasourceBo bo);

    /**
     * 根据数据源名称查询数据源
     *
     * @param datasourceName 数据源名称
     * @return 数据源信息
     */
    SysDatasourceVo queryByDatasourceName(String datasourceName);

    /**
     * 根据数据源类型查询数据源列表
     *
     * @param datasourceType 数据源类型
     * @return 数据源列表
     */
    List<SysDatasourceVo> queryByDatasourceType(String datasourceType);

    /**
     * 查询默认数据源
     *
     * @return 默认数据源
     */
    SysDatasourceVo queryDefaultDatasource();

    /**
     * 设置默认数据源
     *
     * @param datasourceId 数据源ID
     * @return 结果
     */
    Boolean setDefaultDatasource(Long datasourceId);

    /**
     * 校验数据源名称是否唯一
     *
     * @param bo 数据源信息
     * @return 结果
     */
    Boolean checkDatasourceNameUnique(SysDatasourceBo bo);

    /**
     * 校验数据源类型相关字段
     *
     * @param bo 数据源信息
     * @return 校验结果信息
     */
    String validateDatasourceFields(SysDatasourceBo bo);

    /**
     * 根据数据库类型查询数据源列表
     *
     * @param databaseType 数据库类型
     * @return 数据源列表
     */
    List<SysDatasourceVo> queryByDatabaseType(String databaseType);

    /**
     * 查询连接成功的数据源
     *
     * @return 数据源列表
     */
    List<SysDatasourceVo> queryConnectedDatasources();

    /**
     * 统计不同类型数据源数量
     *
     * @param datasourceType 数据源类型
     * @return 数量
     */
    long countByDatasourceType(String datasourceType);

} 