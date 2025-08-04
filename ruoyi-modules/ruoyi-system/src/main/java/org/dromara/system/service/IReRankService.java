package org.dromara.system.service;

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
    List<Double> reRank(List<List<String>> sentencePairs);
}