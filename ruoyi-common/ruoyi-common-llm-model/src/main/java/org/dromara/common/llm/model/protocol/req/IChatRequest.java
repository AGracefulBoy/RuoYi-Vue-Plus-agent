package org.dromara.common.llm.model.protocol.req;

//import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

@Setter
@Getter
public class IChatRequest {
    /**
     * 对话模型
     */
    private String code;

    private Boolean stream;
    /**
     * 对话模型
     */
    private String baseUrl;

    /**
     * 在遇到这些词时，API 将停止生成更多的 token。
     */
    private String apiKey;
    /**
     * 对话模型
     */
    private String model;

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


    public DeepSeekChatOptions deepSeekChatOptions() {
        return DeepSeekChatOptions.builder()
            .model(this.model)
            .maxTokens(this.maxTokens)
            .stop(this.stop)
            .temperature(this.temperature)
            .frequencyPenalty(this.frequencyPenalty)
            .presencePenalty(this.presencePenalty)
            .topP(this.topP)
            .build();
    }

    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
            .model(this.model)
            .maxTokens(this.maxTokens)
            .stop(this.stop)
            .temperature(this.temperature)
            .frequencyPenalty(this.frequencyPenalty)
            .presencePenalty(this.presencePenalty)
            .topP(this.topP)
            .build();
    }

    public DashScopeChatOptions dashScopeChatOptions() {
        return DashScopeChatOptions.builder()
            .withStream(true)
            .withModel(this.model)
            .withMaxToken(this.maxTokens)
            .withTemperature(this.temperature)
            .withTopP(this.topP)
            .build();
    }


    /**
     * 在遇到这些词时，API 将停止生成更多的 token。
     */

    private String completionsPath;

}
