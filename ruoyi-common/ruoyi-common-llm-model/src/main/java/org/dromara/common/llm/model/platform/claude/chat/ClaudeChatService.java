package org.dromara.common.llm.model.platform.claude.chat;


import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.platform.claude.converter.ClaudeChatResponseConverter;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClaudeChatService implements IChatService {
    @Override
    public Flux<IChatResponse> stream(IChatRequest iChatRequest) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(iChatRequest.getApiKey())
            .baseUrl(iChatRequest.getBaseUrl())
            .build();

        OpenAiChatModel built = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(iChatRequest.openAiChatOptions())
            .build();

        List<Message> list = new ArrayList<>();

        if (iChatRequest.getSystemPrompt() != null) {
            SystemMessage systemMessage = new SystemMessage(iChatRequest.getSystemPrompt());
            list.add(systemMessage);
        }

        UserMessage userMessage = new UserMessage(iChatRequest.getPrompt());
        list.add(userMessage);

        return iChatRequest.getStream() ?
            built.stream(new Prompt(list)).map(ClaudeChatResponseConverter::convert) :
            Flux.just(ClaudeChatResponseConverter.convert(built.call(new Prompt(list))));

    }
}
