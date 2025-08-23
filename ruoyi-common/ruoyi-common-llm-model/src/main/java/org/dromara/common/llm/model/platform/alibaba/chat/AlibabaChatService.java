package org.dromara.common.llm.model.platform.alibaba.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;

import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.platform.alibaba.converter.AlibabaChatResponseConverter;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class AlibabaChatService implements IChatService {
    @Override
    public Flux<IChatResponse> stream(IChatRequest iChatRequest) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(iChatRequest.getApiKey())
            .baseUrl(iChatRequest.getBaseUrl())
            .build();

        // 检查是否有媒体内容，决定是否使用视觉模型
        boolean hasMedia = iChatRequest.getMediaContents() != null && !iChatRequest.getMediaContents().isEmpty();

        DashScopeChatOptions chatOptions;
        if (hasMedia) {
            // 视觉模型配置
            chatOptions = DashScopeChatOptions.builder()
                .withStream(iChatRequest.getStream() != null && iChatRequest.getStream())
                .withModel(iChatRequest.getModel())  // 使用视觉模型
                .withMultiModel(true)              // 启用多模态
                .withVlHighResolutionImages(true) // 启用高分辨率图片处理
                .withTemperature(iChatRequest.getTemperature() != null ? iChatRequest.getTemperature() : 0.7)
                .withTopP(iChatRequest.getTopP())
                .withMaxToken(iChatRequest.getMaxTokens())
                .build();
        } else {
            // 普通文本模型配置
            chatOptions = iChatRequest.dashScopeChatOptions();
        }

        DashScopeChatModel built = DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi)
            .defaultOptions(chatOptions)
            .build();

        List<Message> list = new ArrayList<>();

        if (iChatRequest.getSystemPrompt() != null) {
            SystemMessage systemMessage = new SystemMessage(iChatRequest.getSystemPrompt());
            list.add(systemMessage);
        }

        UserMessage userMessage;
        if (hasMedia) {
            // 构建包含图片的用户消息
            List<Media> mediaList = new ArrayList<>();

            for (IChatRequest.MediaContent media : iChatRequest.getMediaContents()) {
                // 判断是URL还是Base64
                if (isUrl(media.getContent())) {
                    try {
                        String mimeType = detectMimeType(media);
                        mediaList.add(new Media(getMimeType(mimeType), 
                            new java.net.URI(media.getContent())));
                    } catch (Exception e) {
                        throw new RuntimeException("Invalid URL: " + media.getContent(), e);
                    }
                } else {
                    // 兼容Base64输入
                    byte[] imageData = Base64.getDecoder().decode(media.getContent());
                    ByteArrayResource resource = new ByteArrayResource(imageData);
                    String mimeType = detectMimeType(media);
                    mediaList.add(new Media(getMimeType(mimeType), resource));
                }
            }

            userMessage = UserMessage.builder()
                .text(iChatRequest.getPrompt())
                .media(mediaList)
                .build();

            // 设置 DashScope 特定的消息格式
            userMessage.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, MessageFormat.IMAGE);
        } else {
            userMessage = new UserMessage(iChatRequest.getPrompt());
        }
        list.add(userMessage);

        return iChatRequest.getStream() ?
            built.stream(new Prompt(list)).map(AlibabaChatResponseConverter::convert) :
            Flux.just(AlibabaChatResponseConverter.convert(built.call(new Prompt(list))));
    }

    private MimeType getMimeType(String mimeTypeStr) {
        if (mimeTypeStr == null) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
        switch (mimeTypeStr.toLowerCase()) {
            case "image/png":
                return MimeTypeUtils.IMAGE_PNG;
            case "image/jpeg":
            case "image/jpg":
                return MimeTypeUtils.IMAGE_JPEG;
            case "image/gif":
                return MimeTypeUtils.IMAGE_GIF;
            default:
                return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
    }
    
    private boolean isUrl(String content) {
        if (content == null) {
            return false;
        }
        return content.startsWith("http://") || content.startsWith("https://");
    }
    
    /**
     * 检测媒体内容的 MIME 类型
     * 优先使用用户提供的 mimeType，如果没有则根据 content 自动检测
     */
    private String detectMimeType(IChatRequest.MediaContent media) {
        // 如果用户已指定 mimeType，直接使用
        if (media.getMimeType() != null && !media.getMimeType().isEmpty()) {
            return media.getMimeType();
        }
        
        // 自动检测
        String content = media.getContent();
        if (content == null) {
            return "application/octet-stream";
        }
        
        if (isUrl(content)) {
            // 从URL扩展名检测
            String url = content.toLowerCase();
            if (url.contains(".png")) {
                return "image/png";
            } else if (url.contains(".jpg") || url.contains(".jpeg")) {
                return "image/jpeg";
            } else if (url.contains(".gif")) {
                return "image/gif";
            } else if (url.contains(".webp")) {
                return "image/webp";
            }
        } else {
            // 从Base64数据检测（通过文件头）
            try {
                byte[] data = Base64.getDecoder().decode(content.substring(0, Math.min(content.length(), 20)));
                if (data.length >= 4) {
                    // PNG: 89 50 4E 47
                    if (data[0] == (byte)0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
                        return "image/png";
                    }
                    // JPEG: FF D8 FF
                    if (data[0] == (byte)0xFF && data[1] == (byte)0xD8 && data[2] == (byte)0xFF) {
                        return "image/jpeg";
                    }
                    // GIF: 47 49 46 38
                    if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38) {
                        return "image/gif";
                    }
                }
            } catch (Exception e) {
                // Base64解码失败，忽略
            }
        }
        
        // 默认返回 JPEG
        return "image/jpeg";
    }
}
