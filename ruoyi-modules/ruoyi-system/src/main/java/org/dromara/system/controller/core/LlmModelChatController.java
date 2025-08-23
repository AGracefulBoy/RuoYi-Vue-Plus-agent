package org.dromara.system.controller.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.dto.ModelChatRequestDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.ISysModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型对话接口
 *
 * @author zhoudashuai
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/model/chat")
public class LlmModelChatController {

    @Autowired
    private ISysModelConfigService modelConfigService;

    // todo 优化
    /**
     * 模型对话，支持流失与非流失
     */
    @PostMapping(path = "/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chat(@RequestBody ModelChatRequestDto modelChatRequestDto) {
        SysModelConfigVo sysModelConfigVo = TenantHelper
            .ignore(() -> modelConfigService.queryById(modelChatRequestDto.getModelId()));

        IChatService chatService = AiService.getChatService(sysModelConfigVo.getModelProvider());

        // 创建IChatRequest对象
        IChatRequest iChatRequest = new IChatRequest();
        iChatRequest.setCode(sysModelConfigVo.getModelCode());
        iChatRequest.setModel(sysModelConfigVo.getModelCode());
        iChatRequest.setApiKey(sysModelConfigVo.getApiKey());
        iChatRequest.setBaseUrl(sysModelConfigVo.getBaseUrl());
        iChatRequest.setStream(modelChatRequestDto.getStream());
        iChatRequest.setPrompt(modelChatRequestDto.getPrompt());
        iChatRequest.setSystemPrompt(modelChatRequestDto.getSystemPrompt());
        iChatRequest.setFrequencyPenalty(modelChatRequestDto.getFrequencyPenalty());
        iChatRequest.setMaxTokens(modelChatRequestDto.getMaxTokens());
        iChatRequest.setPresencePenalty(modelChatRequestDto.getPresencePenalty());
        iChatRequest.setStop(modelChatRequestDto.getStop());
        iChatRequest.setTemperature(modelChatRequestDto.getTemperature());
        iChatRequest.setTopP(modelChatRequestDto.getTopP());
        iChatRequest.setResponseFormat(modelChatRequestDto.getResponseFormat());

        // 转换MediaContent
        if (modelChatRequestDto.getMediaContents() != null && !modelChatRequestDto.getMediaContents().isEmpty()) {
            List<IChatRequest.MediaContent> convertedMediaContents = modelChatRequestDto.getMediaContents().stream()
                .map(dto -> {
                    IChatRequest.MediaContent mediaContent = new IChatRequest.MediaContent();
                    mediaContent.setMimeType(dto.getMimeType());
                    mediaContent.setContent(dto.getContent());
                    return mediaContent;
                })
                .collect(Collectors.toList());
            iChatRequest.setMediaContents(convertedMediaContents);
        }

        if (iChatRequest.getStream() != null && iChatRequest.getStream()) {
            // 流式响应：直接返回 Flux，Spring Boot 会自动处理为 SSE
            return chatService.stream(iChatRequest);
        } else {
            // 非流式响应：返回普通 JSON 对象
            // 确保设置为非流式
            iChatRequest.setStream(false);
            return chatService.stream(iChatRequest).blockFirst();
        }
    }
}
