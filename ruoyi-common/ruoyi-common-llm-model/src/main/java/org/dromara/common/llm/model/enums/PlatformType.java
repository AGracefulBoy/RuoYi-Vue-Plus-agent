package org.dromara.common.llm.model.enums;

// 模型厂商
public enum PlatformType {
    OPENAI("openai"),
    ALIBABA("alibaba"),
    DOUBAO("doubao"),
    DEEPSEEK("deepseek"),
    MOONSHOT("moonshot"),
    HUNYUAN("hunyuan"),
    LINGYI("lingyi"),
    OLLAMA("ollama"),
    MINIMAX("minimax"),
    BAICHUAN("baichuan"),
    ZHIPU("zhipu"),
    CLAUDE("claude"),
    ;

    private final String platform;

    public String getPlatform() {
        return platform;
    }

    PlatformType(String platform) {
        this.platform = platform;
    }

    public static PlatformType getPlatform(String value) {
        String target = value.toLowerCase();
        for (PlatformType platformType : PlatformType.values()) {
            if (platformType.getPlatform().equals(target)) {
                return platformType;
            }
        }
        return PlatformType.OPENAI;
    }
}
