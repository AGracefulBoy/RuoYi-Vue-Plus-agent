package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysKnowledgeBaseBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库管理Service接口
 *
 * @author ruoyi
 */
public interface ISysKnowledgeBaseService {

    /**
     * 查询知识库管理
     *
     * @param knowledgeBaseId 知识库ID
     * @return 知识库管理
     */
    SysKnowledgeBaseVo queryById(Long knowledgeBaseId);

    /**
     * 查询知识库管理列表
     *
     * @param bo        知识库管理
     * @param pageQuery 分页参数
     * @return 知识库管理集合
     */
    TableDataInfo<SysKnowledgeBaseVo> queryPageList(SysKnowledgeBaseBo bo, PageQuery pageQuery);

    /**
     * 查询知识库管理列表
     *
     * @param bo 知识库管理
     * @return 知识库管理集合
     */
    List<SysKnowledgeBaseVo> queryList(SysKnowledgeBaseBo bo);

    /**
     * 新增知识库管理
     *
     * @param bo 知识库管理
     * @return 结果
     */
    Boolean insertByBo(SysKnowledgeBaseBo bo);

    /**
     * 修改知识库管理
     *
     * @param bo 知识库管理
     * @return 结果
     */
    Boolean updateByBo(SysKnowledgeBaseBo bo);

    /**
     * 校验并批量删除知识库管理信息
     *
     * @param ids     需要删除的知识库管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据知识库名称查询知识库管理
     *
     * @param name 知识库名称
     * @return 知识库管理
     */
    SysKnowledgeBaseVo queryByName(String name);

    /**
     * 校验知识库名称是否唯一
     *
     * @param bo 知识库管理
     * @return 结果
     */
    boolean checkNameUnique(SysKnowledgeBaseBo bo);

    /**
     * 修改知识库状态
     *
     * @param knowledgeBaseId 知识库ID
     * @param status          状态
     * @return 结果
     */
    int updateKnowledgeBaseStatus(Long knowledgeBaseId, String status);

} 