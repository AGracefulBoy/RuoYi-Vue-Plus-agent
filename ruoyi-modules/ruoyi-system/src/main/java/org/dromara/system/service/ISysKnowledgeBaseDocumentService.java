package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentSliceUpdateBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseDocumentVo;

import java.util.Collection;
import java.util.List;

/**
 * 知识库文档管理Service接口
 *
 * @author ruoyi
 */
public interface ISysKnowledgeBaseDocumentService {

    /**
     * 查询知识库文档管理
     *
     * @param documentId 文档ID
     * @return 知识库文档管理
     */
    SysKnowledgeBaseDocumentVo queryById(Long documentId);

    /**
     * 查询知识库文档管理列表
     *
     * @param bo        知识库文档管理
     * @param pageQuery 分页参数
     * @return 知识库文档管理集合
     */
    TableDataInfo<SysKnowledgeBaseDocumentVo> queryPageList(SysKnowledgeBaseDocumentBo bo, PageQuery pageQuery);

    /**
     * 查询知识库文档管理列表
     *
     * @param bo 知识库文档管理
     * @return 知识库文档管理集合
     */
    List<SysKnowledgeBaseDocumentVo> queryList(SysKnowledgeBaseDocumentBo bo);

    /**
     * 根据知识库ID查询文档列表
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    List<SysKnowledgeBaseDocumentVo> queryByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 新增知识库文档管理
     *
     * @param bo 知识库文档管理
     * @return 结果
     */
    Boolean insertByBo(SysKnowledgeBaseDocumentBo bo);

    /**
     * 批量新增知识库文档管理
     *
     * @param boList 知识库文档管理列表
     * @return 结果
     */
    Boolean insertByBoList(List<SysKnowledgeBaseDocumentBo> boList);

    /**
     * 修改知识库文档管理
     *
     * @param bo 知识库文档管理
     * @return 结果
     */
    Boolean updateByBo(SysKnowledgeBaseDocumentBo bo);

    /**
     * 校验并批量删除知识库文档管理信息
     *
     * @param ids     需要删除的知识库文档管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 根据知识库ID删除文档
     *
     * @param knowledgeBaseId 知识库ID
     * @return 结果
     */
    Boolean deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 更新文档处理状态
     *
     * @param documentId 文档ID
     * @param status     处理状态
     * @return 结果
     */
    Boolean updateDocumentStatus(Long documentId, String status);

    /**
     * 更新文档切片参数
     *
     * @param bo 文档切片参数更新对象
     * @return 结果
     */
    Boolean updateSliceParams(SysKnowledgeBaseDocumentSliceUpdateBo bo);

    /**
     * 重新切片文档
     * 删除ES中的历史文档并重置文档状态为待执行
     *
     * @param documentId 文档ID
     * @return 操作结果
     */
    Boolean resliceDocument(Long documentId);

} 