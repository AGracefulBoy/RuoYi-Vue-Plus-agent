package org.dromara.common.llm.model.factory;


import org.dromara.common.llm.model.enums.PlatformType;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.platform.IEmbeddingModelService;
import org.dromara.common.llm.model.platform.IRerankModeService;
import org.dromara.common.llm.model.platform.alibaba.chat.AlibabaChatService;
import org.dromara.common.llm.model.platform.alibaba.embedding.AlibabaEmbeddingService;
import org.dromara.common.llm.model.platform.alibaba.rerank.AlibabaRerankService;
import org.dromara.common.llm.model.platform.claude.chat.ClaudeChatService;
import org.dromara.common.llm.model.platform.deepseek.chat.DeepSeekChatService;
import org.dromara.common.llm.model.platform.doubao.chat.DouBaoChatService;
import org.dromara.common.llm.model.platform.moonshot.chat.MoonshotChatService;
import org.dromara.common.llm.model.platform.openai.chat.OpenAiChatService;
import org.dromara.common.llm.model.platform.openai.embedding.OpenAiEmbeddingService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * AiService 工厂，可以根据 PlatformType 选择不同平台的AI 能力
 * 使用Spring依赖注入获取服务bean，提高性能
 */
@Component
public class AiService implements InitializingBean {

    /**
     * 服务实例缓存Map，使用EnumMap提高性能
     */
    private static final Map<PlatformType, IChatService> SERVICE_CACHE = new EnumMap<>(PlatformType.class);

    /**
     * Embedding服务实例缓存Map，使用EnumMap提高性能
     */
    private static final Map<PlatformType, IEmbeddingModelService> EMBEDDING_SERVICE_CACHE = new EnumMap<>(PlatformType.class);

    /**
     * Rerank服务实例缓存Map，使用EnumMap提高性能
     */
    private static final Map<PlatformType, IRerankModeService> RERANK_SERVICE_CACHE = new EnumMap<>(PlatformType.class);

    /**
     * 注入OpenAI聊天服务
     */
    @Autowired
    private OpenAiChatService openAiChatService;

    /**
     * 注入DeepSeek聊天服务
     */
    @Autowired(required = false)
    private DeepSeekChatService deepSeekChatService;

    @Autowired(required = false)
    private AlibabaChatService alibabaChatService;

    @Autowired(required = false)
    private DouBaoChatService douBaoChatService;

    @Autowired(required = false)
    private MoonshotChatService moonshotChatService;

    @Autowired(required = false)
    private ClaudeChatService claudeChatService;

    /**
     * 注入OpenAI Embedding服务
     */
    @Autowired(required = false)
    private OpenAiEmbeddingService openAiEmbeddingService;

    /**
     * 注入Alibaba Embedding服务
     */
    @Autowired(required = false)
    private AlibabaEmbeddingService alibabaEmbeddingService;

    /**
     * 注入Alibaba Rerank服务
     */
    @Autowired(required = false)
    private AlibabaRerankService alibabaRerankService;


    /**
     * Spring容器初始化后，建立平台类型与服务bean的映射关系
     */
    @Override
    public void afterPropertiesSet() {
        // 建立平台类型与服务bean的映射
        if (openAiChatService != null) {
            SERVICE_CACHE.put(PlatformType.OPENAI, openAiChatService);
        }
        if (deepSeekChatService != null) {
            SERVICE_CACHE.put(PlatformType.DEEPSEEK, deepSeekChatService);
        }

        if (alibabaChatService != null) {
            SERVICE_CACHE.put(PlatformType.ALIBABA, alibabaChatService);
        }

        if (douBaoChatService != null) {
            SERVICE_CACHE.put(PlatformType.DOUBAO, douBaoChatService);
        }

        if (moonshotChatService != null) {
            SERVICE_CACHE.put(PlatformType.MOONSHOT, moonshotChatService);
        }

        if (claudeChatService != null) {
            SERVICE_CACHE.put(PlatformType.CLAUDE, claudeChatService);
        }

        // 建立平台类型与Embedding服务bean的映射
        if (openAiEmbeddingService != null) {
            EMBEDDING_SERVICE_CACHE.put(PlatformType.OPENAI, openAiEmbeddingService);
        }
        if (alibabaEmbeddingService != null) {
            EMBEDDING_SERVICE_CACHE.put(PlatformType.ALIBABA, alibabaEmbeddingService);
        }

        // 建立平台类型与Rerank服务bean的映射
        if (alibabaRerankService != null) {
            RERANK_SERVICE_CACHE.put(PlatformType.ALIBABA, alibabaRerankService);
        }
    }

    /**
     * 根据平台类型获取对应的聊天服务（从Spring容器获取bean）
     *
     * @param platformType 平台类型
     * @return 对应的聊天服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IChatService getChatService(PlatformType platformType) {
        // 先从缓存中获取
        IChatService service = SERVICE_CACHE.get(platformType);
        if (service != null) {
            return service;
        }
        // 其余平台均已openai 作为支持
        return SERVICE_CACHE.get(PlatformType.OPENAI);
    }

    /**
     * 根据平台名称获取对应的聊天服务（从Spring容器获取bean）
     *
     * @param platformName 平台名称
     * @return 对应的聊天服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IChatService getChatService(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return getChatService(platformType);
    }

    /**
     * 获取所有已支持的平台类型
     *
     * @return 支持的平台类型数组
     */
    public static PlatformType[] getSupportedPlatforms() {
        return SERVICE_CACHE.keySet().toArray(new PlatformType[0]);
    }

    /**
     * 检查指定平台是否支持
     *
     * @param platformType 平台类型
     * @return 是否支持
     */
    public static boolean isSupported(PlatformType platformType) {
        return SERVICE_CACHE.containsKey(platformType);
    }

    /**
     * 检查指定平台名称是否支持
     *
     * @param platformName 平台名称
     * @return 是否支持
     */
    public static boolean isSupported(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return isSupported(platformType);
    }

    /**
     * 根据平台类型获取对应的Embedding服务（从Spring容器获取bean）
     *
     * @param platformType 平台类型
     * @return 对应的Embedding服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IEmbeddingModelService getEmbeddingService(PlatformType platformType) {
        // 先从缓存中获取
        IEmbeddingModelService service = EMBEDDING_SERVICE_CACHE.get(platformType);
        if (service != null) {
            return service;
        }
        // 默认使用OpenAI embedding服务
        return EMBEDDING_SERVICE_CACHE.get(PlatformType.OPENAI);
    }

    /**
     * 根据平台名称获取对应的Embedding服务（从Spring容器获取bean）
     *
     * @param platformName 平台名称
     * @return 对应的Embedding服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IEmbeddingModelService getEmbeddingService(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return getEmbeddingService(platformType);
    }

    /**
     * 获取所有已支持的Embedding平台类型
     *
     * @return 支持的Embedding平台类型数组
     */
    public static PlatformType[] getSupportedEmbeddingPlatforms() {
        return EMBEDDING_SERVICE_CACHE.keySet().toArray(new PlatformType[0]);
    }

    /**
     * 检查指定平台是否支持Embedding
     *
     * @param platformType 平台类型
     * @return 是否支持
     */
    public static boolean isEmbeddingSupported(PlatformType platformType) {
        return EMBEDDING_SERVICE_CACHE.containsKey(platformType);
    }

    /**
     * 检查指定平台名称是否支持Embedding
     *
     * @param platformName 平台名称
     * @return 是否支持
     */
    public static boolean isEmbeddingSupported(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return isEmbeddingSupported(platformType);
    }

    /**
     * 根据平台类型获取对应的Rerank服务（从Spring容器获取bean）
     *
     * @param platformType 平台类型
     * @return 对应的Rerank服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IRerankModeService getRerankService(PlatformType platformType) {
        // 先从缓存中获取
        IRerankModeService service = RERANK_SERVICE_CACHE.get(platformType);
        if (service != null) {
            return service;
        }
        // 如果指定平台不支持，抛出异常
        throw new UnsupportedOperationException("不支持的Rerank平台: " + platformType);
    }

    /**
     * 根据平台名称获取对应的Rerank服务（从Spring容器获取bean）
     *
     * @param platformName 平台名称
     * @return 对应的Rerank服务实现
     * @throws UnsupportedOperationException 当平台类型不支持时抛出异常
     */
    public static IRerankModeService getRerankService(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return getRerankService(platformType);
    }

    /**
     * 获取所有已支持的Rerank平台类型
     *
     * @return 支持的Rerank平台类型数组
     */
    public static PlatformType[] getSupportedRerankPlatforms() {
        return RERANK_SERVICE_CACHE.keySet().toArray(new PlatformType[0]);
    }

    /**
     * 检查指定平台是否支持Rerank
     *
     * @param platformType 平台类型
     * @return 是否支持
     */
    public static boolean isRerankSupported(PlatformType platformType) {
        return RERANK_SERVICE_CACHE.containsKey(platformType);
    }

    /**
     * 检查指定平台名称是否支持Rerank
     *
     * @param platformName 平台名称
     * @return 是否支持
     */
    public static boolean isRerankSupported(String platformName) {
        PlatformType platformType = PlatformType.getPlatform(platformName);
        return isRerankSupported(platformType);
    }
}
