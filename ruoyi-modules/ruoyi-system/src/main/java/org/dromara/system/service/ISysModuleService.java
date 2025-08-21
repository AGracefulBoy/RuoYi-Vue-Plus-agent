package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysModuleBo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.domain.vo.SysModuleVo;

import java.util.Collection;
import java.util.List;

/**
 * 系统模块Service接口
 *
 * @author 系统管理员
 */
public interface ISysModuleService {

    /**
     * 查询系统模块
     *
     * @param moduleId 模块ID
     * @return 系统模块
     */
    SysModuleVo queryById(Long moduleId);

    /**
     * 查询系统模块列表
     *
     * @param module 系统模块
     * @param pageQuery 分页参数
     * @return 系统模块集合
     */
    TableDataInfo<SysModuleVo> queryPageList(SysModuleBo module, PageQuery pageQuery);

    /**
     * 查询系统模块列表
     *
     * @param module 系统模块
     * @return 系统模块集合
     */
    List<SysModuleVo> queryList(SysModuleBo module);

    /**
     * 新增系统模块
     *
     * @param bo 系统模块
     * @return 是否新增成功
     */
    Boolean insertByBo(SysModuleBo bo);

    /**
     * 修改系统模块
     *
     * @param bo 系统模块
     * @return 是否修改成功
     */
    Boolean updateByBo(SysModuleBo bo);

    /**
     * 批量删除系统模块
     *
     * @param ids 需要删除的模块ID集合
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 根据模块编码查询模块信息
     *
     * @param moduleCode 模块编码
     * @return 模块信息
     */
    SysModuleVo queryByModuleCode(String moduleCode);

    /**
     * 根据模块ID查询关联的模型列表
     *
     * @param moduleId 模块ID
     * @return 模型列表
     */
    List<SysModelConfigVo> queryModelsByModuleId(Long moduleId);

    /**
     * 根据模块编码查询关联的模型列表
     *
     * @param moduleCode 模块编码
     * @return 模型列表
     */
    List<SysModelConfigVo> queryModelsByModuleCode(String moduleCode);
}