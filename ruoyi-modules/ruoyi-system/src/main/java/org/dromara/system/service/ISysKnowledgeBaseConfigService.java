package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysKnowledgeBaseConfigBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseConfigVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库默认配置Service接口
 *
 * @author ruoyi
 */
public interface ISysKnowledgeBaseConfigService {

    /**
     * 查询知识库默认配置
     *
     * @param knowledgeBaseConfigId 知识库默认配置主键
     * @return 知识库默认配置
     */
    SysKnowledgeBaseConfigVo queryById(Long knowledgeBaseConfigId);

    /**
     * 查询知识库默认配置列表
     *
     * @param bo        知识库默认配置
     * @param pageQuery 分页参数
     * @return 知识库默认配置集合
     */
    TableDataInfo<SysKnowledgeBaseConfigVo> queryPageList(SysKnowledgeBaseConfigBo bo, PageQuery pageQuery);

    /**
     * 查询知识库默认配置列表
     *
     * @param bo 知识库默认配置
     * @return 知识库默认配置集合
     */
    List<SysKnowledgeBaseConfigVo> queryList(SysKnowledgeBaseConfigBo bo);

    /**
     * 新增知识库默认配置
     *
     * @param bo 知识库默认配置
     * @return 结果
     */
    Boolean insertByBo(SysKnowledgeBaseConfigBo bo);

    /**
     * 修改知识库默认配置
     *
     * @param bo 知识库默认配置
     * @return 结果
     */
    Boolean updateByBo(SysKnowledgeBaseConfigBo bo);

    /**
     * 校验并批量删除知识库默认配置信息
     *
     * @param ids     需要删除的知识库默认配置主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 获取默认配置
     *
     * @return 默认配置
     */
    SysKnowledgeBaseConfigVo getDefaultConfig();

} 