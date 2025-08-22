package org.dromara.common.llm.model.platform.alibaba.rerank;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import org.dromara.common.llm.model.platform.IRerankModeService;
import org.dromara.common.llm.model.protocol.req.IReRankRequest;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AlibabaRerankService implements IRerankModeService {
    public RerankResponse call(IReRankRequest iReRankRequest) {
        try {
            log.info("开始执行rerank调用，模型: {}, topN: {}", iReRankRequest.getModel(), iReRankRequest.getTopN());

            // 构建DashScope API客户端
            DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(iReRankRequest.getApiKey())
                .baseUrl(iReRankRequest.getBaseUrl())
                .build();

            DashScopeRerankModel rerankModel = new DashScopeRerankModel(dashScopeApi);

            // 构建rerank选项
            DashScopeRerankOptions options = DashScopeRerankOptions.builder()
                .withModel(iReRankRequest.getModel())
                .withTopN(iReRankRequest.getTopN())
                .build();

            // 将字符串列表转换为Document列表
            List<Document> documents = iReRankRequest.getDocuments().stream()
                .map(Document::new)
                .collect(Collectors.toList());

            // 执行rerank调用
            RerankResponse response = rerankModel.call(new RerankRequest(iReRankRequest.getQuery(), documents, options));

            log.info("rerank调用成功完成，返回结果数量: {}", response.getResults().size());
            return response;

        } catch (Exception e) {
            log.error("rerank调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("rerank调用失败: " + e.getMessage(), e);
        }
    }
}
