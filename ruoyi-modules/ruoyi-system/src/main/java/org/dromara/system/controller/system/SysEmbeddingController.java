package org.dromara.system.controller.system;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.service.IEmbeddingService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Embedding management controller.
 * Provides APIs for text vectorization operations.
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/embedding")
public class SysEmbeddingController extends BaseController {

    private final IEmbeddingService embeddingService;

    /**
     * Convert single text to embedding vector.
     */
    @PostMapping("/text")
    @Log(title = "文本向量化", businessType = BusinessType.OTHER)
    public R<List<Double>> textToEmbedding(@RequestParam String text) {
        try {
            List<Double> embedding = embeddingService.textToEmbedding(text);
            return R.ok(embedding);
        } catch (Exception exception) {
            return R.fail("文本向量化失败: " + exception.getMessage());
        }
    }

    /**
     * Convert multiple texts to embedding vectors.
     */
    @PostMapping("/texts")
    @Log(title = "批量文本向量化", businessType = BusinessType.OTHER)
    public R<List<List<Double>>> textsToEmbeddings(@RequestBody List<String> texts) {
        try {
            List<List<Double>> embeddings = embeddingService.textsToEmbeddings(texts);
            return R.ok(embeddings);
        } catch (Exception exception) {
            return R.fail("批量文本向量化失败: " + exception.getMessage());
        }
    }

    /**
     * Convert single text to embedding array (for Elasticsearch).
     */
    @PostMapping("/array")
    @Log(title = "文本向量化数组", businessType = BusinessType.OTHER)
    public R<float[]> textToEmbeddingArray(@RequestParam String text) {
        try {
            float[] embeddingArray = embeddingService.textToEmbeddingArray(text);
            return R.ok(embeddingArray);
        } catch (Exception exception) {
            return R.fail("文本向量化数组失败: " + exception.getMessage());
        }
    }

    /**
     * Check if embedding service is available.
     */
    @GetMapping("/status")
    public R<Boolean> checkServiceStatus() {
        try {
            Boolean isAvailable = embeddingService.isServiceAvailable();
            return R.ok(isAvailable);
        } catch (Exception exception) {
            return R.fail("检查服务状态失败: " + exception.getMessage());
        }
    }
}