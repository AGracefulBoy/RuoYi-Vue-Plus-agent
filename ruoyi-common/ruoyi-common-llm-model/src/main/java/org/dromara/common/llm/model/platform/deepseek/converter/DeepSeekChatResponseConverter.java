package org.dromara.common.llm.model.platform.deepseek.converter;


import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;

public class DeepSeekChatResponseConverter {
    /**
     * 将ChatResponse转换为IChatResponse
     *
     * @param chatResponse Spring AI的ChatResponse对象
     * @return 转换后的IChatResponse对象
     */
    public static IChatResponse convert(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        }

        IChatResponse iChatResponse = new IChatResponse();

        // 创建Result对象
        IChatResponse.Result result = new IChatResponse.Result();
        // 创建Output对象
        IChatResponse.Output output = new IChatResponse.Output();
        // 设置文本内容
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null && chatResponse.getResult().getOutput() instanceof DeepSeekAssistantMessage) {
            output.setReasoningContent(((DeepSeekAssistantMessage) chatResponse.getResult().getOutput()).getReasoningContent());
            output.setPrefix(((DeepSeekAssistantMessage) chatResponse.getResult().getOutput()).getPrefix());
            output.setMessageType("text");
        }
        // 设置文本内容
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            output.setText(chatResponse.getResult().getOutput().getText());
            output.setMessageType("text");
        }

        // 创建OutputMetadata对象
        IChatResponse.OutputMetadata outputMetadata = new IChatResponse.OutputMetadata();
        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            outputMetadata.setFinishReason(chatResponse.getResult().getMetadata().getFinishReason());
            outputMetadata.setRole("assistant");
            outputMetadata.setMessageType("text");
        }
        output.setMetadata(outputMetadata);

        // 创建ResultMetadata对象
        IChatResponse.ResultMetadata resultMetadata = new IChatResponse.ResultMetadata();
        if (chatResponse.getResult() != null && chatResponse.getResult().getMetadata() != null) {
            resultMetadata.setFinishReason(chatResponse.getResult().getMetadata().getFinishReason());
        }

        result.setOutput(output);
        result.setMetadata(resultMetadata);
        iChatResponse.setResult(result);

        // 创建Metadata对象
        IChatResponse.Metadata metadata = new IChatResponse.Metadata();
        if (chatResponse.getMetadata() != null) {
            metadata.setId(chatResponse.getMetadata().getId());
            metadata.setModel(chatResponse.getMetadata().getModel());

            // 创建Usage对象
            IChatResponse.Usage usage = new IChatResponse.Usage();
            if (chatResponse.getMetadata().getUsage() != null) {
                usage.setPromptTokens(chatResponse.getMetadata().getUsage().getPromptTokens());
                usage.setCompletionTokens(chatResponse.getMetadata().getUsage().getCompletionTokens());
                usage.setTotalTokens(chatResponse.getMetadata().getUsage().getTotalTokens());
            }
            metadata.setUsage(usage);

            // 创建RateLimit对象（如果需要的话）
            IChatResponse.RateLimit rateLimit = new IChatResponse.RateLimit();
            // 由于ChatResponse中可能没有直接的rateLimit信息，这里可以设置默认值或者留空
            metadata.setRateLimit(rateLimit);
        }

        iChatResponse.setMetadata(metadata);

        return iChatResponse;
    }
}
