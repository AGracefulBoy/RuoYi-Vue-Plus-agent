package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysModelConfigBo;
import org.dromara.system.domain.vo.SysModelConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 模型配置Service接口
 *
 * @author 系统管理员
 */
public interface ISysModelConfigService {

    /**
     * 查询模型配置
     */
    SysModelConfigVo queryById(Long id);

    /**
     * 查询模型配置列表
     */
    TableDataInfo<SysModelConfigVo> queryPageList(SysModelConfigBo bo, PageQuery pageQuery);

    /**
     * 查询模型配置列表
     */
    List<SysModelConfigVo> queryList(SysModelConfigBo bo);

    /**
     * 新增模型配置
     */
    Boolean insertByBo(SysModelConfigBo bo);

    /**
     * 修改模型配置
     */
    Boolean updateByBo(SysModelConfigBo bo);

    /**
     * 校验并批量删除模型配置信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据模型编码查询模型配置
     */
    SysModelConfigVo queryByModelCode(String modelCode);

    /**
     * 根据模型厂商查询模型配置列表
     */
    List<SysModelConfigVo> queryByModelProvider(String modelProvider);

    /**
     * 根据模型类型查询模型配置列表
     */
    List<SysModelConfigVo> queryByModelType(String modelType);

} 