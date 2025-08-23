package org.dromara.system.service;

import org.dromara.system.domain.vo.SysKnowledgeBaseVo;

import java.util.List;

/**
 * ReRank服务接口
 */
public interface IReRankService {

    /**
     * 对句子对进行重排序
     *
     * @param sentencePairs 句子对列表
     * @return 相似度分数列表
     */
    List<Double> reRank(String question, List<String> sentencePairs, SysKnowledgeBaseVo knowledgeBase);
}
