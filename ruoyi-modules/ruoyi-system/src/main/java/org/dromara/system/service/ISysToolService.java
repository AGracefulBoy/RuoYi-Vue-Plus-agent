package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;

import java.util.Collection;
import java.util.List;

/**
 * 工具管理Service接口
 *
 * @author ruoyi
 */
public interface ISysToolService {

    /**
     * 查询工具管理（包含脚本代码）
     *
     * @param toolId 工具ID
     * @return 工具管理
     */
    SysToolVo queryById(Long toolId);

    /**
     * 查询工具管理列表（不包含脚本代码）
     *
     * @param bo        工具管理
     * @param pageQuery 分页参数
     * @return 工具管理集合
     */
    TableDataInfo<SysToolListVo> queryPageList(SysToolBo bo, PageQuery pageQuery);

    /**
     * 查询工具管理列表（不包含脚本代码）
     *
     * @param bo 工具管理
     * @return 工具管理集合
     */
    List<SysToolListVo> queryList(SysToolBo bo);

    /**
     * 新增工具管理
     *
     * @param bo 工具管理
     * @return 结果
     */
    Boolean insertByBo(SysToolBo bo);

    /**
     * 修改工具管理
     *
     * @param bo 工具管理
     * @return 结果
     */
    Boolean updateByBo(SysToolBo bo);

    /**
     * 校验并批量删除工具管理信息
     *
     * @param ids     需要删除的工具管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验工具名称是否唯一
     *
     * @param bo 工具管理
     * @return 结果
     */
    boolean checkToolNameUnique(SysToolBo bo);

    /**
     * 根据工具名称查询工具管理
     *
     * @param toolName 工具名称
     * @return 工具管理
     */
    SysToolVo queryByToolName(String toolName);

    /**
     * 根据工具类型查询工具管理列表
     *
     * @param toolType 工具类型
     * @return 工具管理集合
     */
    List<SysToolVo> queryByToolType(String toolType);

    /**
     * 根据工具状态查询工具管理列表
     *
     * @param toolStatus 工具状态
     * @return 工具管理集合
     */
    List<SysToolVo> queryByToolStatus(String toolStatus);

    /**
     * 更新工具状态
     *
     * @param toolId     工具ID
     * @param toolStatus 工具状态
     * @return 结果
     */
    Boolean updateToolStatus(Long toolId, String toolStatus);

} 