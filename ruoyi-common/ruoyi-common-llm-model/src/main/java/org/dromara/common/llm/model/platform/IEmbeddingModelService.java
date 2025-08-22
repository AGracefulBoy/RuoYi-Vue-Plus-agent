package org.dromara.common.llm.model.platform;

import org.dromara.common.llm.model.protocol.req.IEmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * embedding
 */
public interface IEmbeddingModelService {

    public EmbeddingResponse call(IEmbeddingRequest iEmbeddingRequest);
}
