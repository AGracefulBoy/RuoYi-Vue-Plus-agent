package org.dromara.system.domain.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.req.ResponseFormatRequest;

import java.util.List;

/**
 * 聊天请求DTO
 *
 * @author zhoudashuai
 */
@Data
public class ModelChatRequestDto {

    /**
     * 对话模型id
     */
    private Long modelId;

    private Boolean stream;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其在已有文本中的出现频率受到相应的惩罚，降低模型重复相同内容的可能性
     */
    private Double frequencyPenalty;
    /**
     * 介于 1 到 8192 间的整数，限制一次请求中模型生成 completion 的最大 token 数。输入 token 和输出 token 的总长度受模型的上下文长度的限制
     */
    private Integer maxTokens;

    /**
     * 介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其是否已在已有文本中出现受到相应的惩罚，从而增加模型谈论新主题的可能性。
     */
    private Double presencePenalty;
    /**
     * 一个 string 或最多包含 16 个 string 的 list，在遇到这些词时，API 将停止生成更多的 token。
     */
    private List<String> stop;
    /**
     * 采样温度，介于 0 和 2 之间。更高的值，如 0.8，会使输出更随机，而更低的值，如 0.2，会使其更加集中和确定。 我们通常建议可以更改这个值或者更改 top_p，但不建议同时对两者进行修改
     */
    private Double temperature;
    /**
     * 作为调节采样温度的替代方案，模型会考虑前 top_p 概率的 token 的结果。所以 0.1 就意味着只有包括在最高 10% 概率中的 token 会被考虑。 我们通常建议修改这个值或者更改 temperature，但不建议同时对两者进行修改。
     */
    private Double topP;


    private ResponseFormatRequest responseFormat;

    /**
     * 媒体内容列表（用于视觉模型）
     */
    private List<MediaContent> mediaContents;

    @Setter
    @Getter
    public static class MediaContent {
        private String mimeType;    // "image/png", "image/jpeg" 等，可选，不传则自动检测
        private String content;      // 图片URL地址
    }
}
