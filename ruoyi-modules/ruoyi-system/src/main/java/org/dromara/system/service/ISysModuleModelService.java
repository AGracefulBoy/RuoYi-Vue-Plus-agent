package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysModuleModelBo;
import org.dromara.system.domain.vo.SysModuleModelVo;

import java.util.Collection;
import java.util.List;

/**
 * 模块模型关联Service接口
 *
 * @author 系统管理员
 */
public interface ISysModuleModelService {

    /**
     * 查询模块模型关联
     *
     * @param id 主键
     * @return 模块模型关联
     */
    SysModuleModelVo queryById(Long id);

    /**
     * 查询模块模型关联列表
     *
     * @param bo 模块模型关联
     * @param pageQuery 分页参数
     * @return 模块模型关联集合
     */
    TableDataInfo<SysModuleModelVo> queryPageList(SysModuleModelBo bo, PageQuery pageQuery);

    /**
     * 查询模块模型关联列表
     *
     * @param bo 模块模型关联
     * @return 模块模型关联集合
     */
    List<SysModuleModelVo> queryList(SysModuleModelBo bo);

    /**
     * 新增模块模型关联
     *
     * @param bo 模块模型关联
     * @return 是否新增成功
     */
    Boolean insertByBo(SysModuleModelBo bo);

    /**
     * 修改模块模型关联
     *
     * @param bo 模块模型关联
     * @return 是否修改成功
     */
    Boolean updateByBo(SysModuleModelBo bo);

    /**
     * 批量删除模块模型关联
     *
     * @param ids 需要删除的主键集合
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 绑定模块与模型关系
     *
     * @param moduleId 模块ID
     * @param modelIds 模型ID列表
     * @return 是否绑定成功
     */
    Boolean bindModuleModels(Long moduleId, List<Long> modelIds);

    /**
     * 解绑模块与模型关系
     *
     * @param moduleId 模块ID
     * @param modelIds 模型ID列表
     * @return 是否解绑成功
     */
    Boolean unbindModuleModels(Long moduleId, List<Long> modelIds);

    /**
     * 设置模块的默认模型
     *
     * @param moduleId 模块ID
     * @param modelId 模型ID
     * @return 是否设置成功
     */
    Boolean setDefaultModel(Long moduleId, Long modelId);

    /**
     * 根据模块ID查询默认模型
     *
     * @param moduleId 模块ID
     * @return 默认模型ID，如果没有找到则返回null
     */
    Long queryDefaultModelByModuleId(Long moduleId);
    
    /**
     * 批量根据模块ID查询默认模型
     *
     * @param moduleIds 模块ID列表
     * @return 模块ID与默认模型ID的映射，key为模块ID，value为默认模型ID
     */
    java.util.Map<Long, Long> queryDefaultModelsByModuleIds(List<Long> moduleIds);

}
