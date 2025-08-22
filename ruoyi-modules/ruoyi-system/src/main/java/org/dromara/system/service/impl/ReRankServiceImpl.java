package org.dromara.system.service.impl;

import com.alibaba.cloud.ai.model.RerankResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IRerankModeService;
import org.dromara.common.llm.model.protocol.req.IReRankRequest;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.IReRankService;
import org.dromara.system.service.ISysModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ReRank服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ReRankServiceImpl implements IReRankService {

    @Autowired
    private ISysModelConfigService modelConfigService;


    @Override
    public List<Double> reRank(List<String> sentencePairs, SysKnowledgeBaseVo knowledgeBase) {
        try {
            // 参数验证
            if (CollectionUtils.isEmpty(sentencePairs)) {
                log.warn("sentencePairs为空，返回空结果");
                return Collections.emptyList();
            }

            if (sentencePairs.size() < 2) {
                log.warn("sentencePairs至少需要包含查询和文档，当前大小: {}", sentencePairs.size());
                return Collections.emptyList();
            }

            // 获取模型配置
            SysModelConfigVo modelConfig = modelConfigService.queryById(knowledgeBase.getRerankModel());
            if (modelConfig == null) {
                throw new RuntimeException("无法找到rerank模型配置，modelId: " + knowledgeBase.getRerankModel());
            }

            log.info("开始执行rerank，模型提供商: {}, 模型: {}, 文档数量: {}",
                modelConfig.getModelProvider(), modelConfig.getModelCode(), sentencePairs.size() - 1);

            // 获取rerank服务
            IRerankModeService rerankService = AiService.getRerankService(modelConfig.getModelProvider());

            // 解析sentencePairs参数：第一个元素是查询，其余元素是文档
            String query = sentencePairs.get(0);
            List<String> documents = sentencePairs.subList(1, sentencePairs.size());

            // 构建rerank请求
            IReRankRequest rerankRequest = IReRankRequest.builder()
                .apiKey(modelConfig.getApiKey())
                .baseUrl(modelConfig.getBaseUrl())
                .model(modelConfig.getModelCode())
                .topN(knowledgeBase.getTopK() != null ? knowledgeBase.getTopK() : documents.size())
                .query(query)
                .documents(documents)
                .build();

            // 执行rerank调用
            RerankResponse response = rerankService.call(rerankRequest);

            // 处理响应结果，提取分数
            List<Double> scores = new ArrayList<>();
            if (response != null && response.getResults() != null) {
                response.getResults().forEach(result -> {
                    // 添加相关性分数，如果没有分数则使用0.0
                    Double score = result.getScore() != null ? result.getScore() : 0.0;
                    scores.add(score);
                });

                log.info("rerank调用成功，返回{}个结果的分数", scores.size());
            } else {
                log.warn("rerank调用返回空结果");
                // 返回默认分数（全部为0）
                for (int i = 0; i < documents.size(); i++) {
                    scores.add(0.0);
                }
            }

            return scores;

        } catch (Exception e) {
            log.error("rerank调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("rerank调用失败: " + e.getMessage(), e);
        }
    }
}
