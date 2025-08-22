package org.dromara.common.llm.model.platform;

import com.alibaba.cloud.ai.model.RerankResponse;
import org.dromara.common.llm.model.protocol.req.IReRankRequest;

/**
 * Rerank服务接口
 */
public interface IRerankModeService {

    /**
     * 执行重新排序
     *
     * @param iReRankRequest 重排序请求参数
     * @return 重排序响应
     */
    RerankResponse call(IReRankRequest iReRankRequest);
}
