package org.dromara.common.llm.model.platform.moonshot.chat;


import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.platform.deepseek.converter.DeepSeekChatResponseConverter;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class MoonshotChatService implements IChatService {
    @Override
    public Flux<IChatResponse> stream(IChatRequest iChatRequest) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
            .apiKey(iChatRequest.getApiKey())
            .baseUrl(iChatRequest.getBaseUrl())
            .completionsPath(iChatRequest.getCompletionsPath())
            .build();

        DeepSeekChatModel built = DeepSeekChatModel.builder()
            .deepSeekApi(deepSeekApi)
            .defaultOptions(
                iChatRequest.deepSeekChatOptions()
            )
            .build();
        List<Message> list = new ArrayList<>();

        if (iChatRequest.getSystemPrompt() != null) {
            SystemMessage systemMessage = new SystemMessage(iChatRequest.getSystemPrompt());
            list.add(systemMessage);
        }

        UserMessage userMessage = new UserMessage(iChatRequest.getPrompt());
        list.add(userMessage);
        return iChatRequest.getStream() ?
            built.stream(new Prompt(list)).map(DeepSeekChatResponseConverter::convert) :
            Flux.just(DeepSeekChatResponseConverter.convert(built.call(new Prompt(list))));

    }
}
