package org.dromara.system.domain.context;

import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.ToolDto;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * 流式处理上下文
 * 用于在流式处理过程中维护状态和数据
 * 
 * @author TaskAgentService
 */
public class StreamingContext {
    private StringBuilder conversationHistory = new StringBuilder();
    private StringBuilder promptChain = new StringBuilder();  // 记录完整的提示词链
    private Integer messageIndexCounter = 0;
    private String currentChatId;
    private String userMessageId;
    private StringBuilder streamBuffer = new StringBuilder();
    private String currentStreamType = "thought";
    private boolean actionDetected = false;
    private boolean finalAnswerDetected = false;
    private List<ToolDto> currentAvailableTools;
    private IChatResponse.Usage totalTokenUsage;
    private ModelConfigContext modelContext;
    private StringBuilder thoughtProcess = new StringBuilder();
    private boolean shouldStopCurrentStream = false;
    private FluxSink<StreamMessageResponseDto> currentSink;

    public StreamingContext() {
        this.totalTokenUsage = new IChatResponse.Usage();
        this.totalTokenUsage.setPromptTokens(0);
        this.totalTokenUsage.setCompletionTokens(0);
        this.totalTokenUsage.setTotalTokens(0);
    }

    public void resetStreamBufferState(String initialType) {
        streamBuffer.setLength(0);
        currentStreamType = initialType;
        actionDetected = false;
        finalAnswerDetected = false;
    }

    public int getAndIncrementMessageIndex() {
        return messageIndexCounter++;
    }

    // Getters and setters
    public StringBuilder getConversationHistory() {
        return conversationHistory;
    }
    
    public StringBuilder getPromptChain() {
        return promptChain;
    }

    public String getCurrentChatId() {
        return currentChatId;
    }

    public void setCurrentChatId(String currentChatId) {
        this.currentChatId = currentChatId;
    }

    public String getUserMessageId() {
        return userMessageId;
    }

    public void setUserMessageId(String userMessageId) {
        this.userMessageId = userMessageId;
    }

    public StringBuilder getStreamBuffer() {
        return streamBuffer;
    }

    public String getCurrentStreamType() {
        return currentStreamType;
    }

    public void setCurrentStreamType(String currentStreamType) {
        this.currentStreamType = currentStreamType;
    }

    public boolean isActionDetected() {
        return actionDetected;
    }

    public void setActionDetected(boolean actionDetected) {
        this.actionDetected = actionDetected;
    }

    public boolean isFinalAnswerDetected() {
        return finalAnswerDetected;
    }

    public void setFinalAnswerDetected(boolean finalAnswerDetected) {
        this.finalAnswerDetected = finalAnswerDetected;
    }

    public List<ToolDto> getCurrentAvailableTools() {
        return currentAvailableTools;
    }

    public void setCurrentAvailableTools(List<ToolDto> currentAvailableTools) {
        this.currentAvailableTools = currentAvailableTools;
    }

    public IChatResponse.Usage getTotalTokenUsage() {
        return totalTokenUsage;
    }

    public ModelConfigContext getModelContext() {
        return modelContext;
    }

    public void setModelContext(ModelConfigContext modelContext) {
        this.modelContext = modelContext;
    }

    public StringBuilder getThoughtProcess() {
        return thoughtProcess;
    }

    public boolean isShouldStopCurrentStream() {
        return shouldStopCurrentStream;
    }

    public void setShouldStopCurrentStream(boolean shouldStopCurrentStream) {
        this.shouldStopCurrentStream = shouldStopCurrentStream;
    }

    public FluxSink<StreamMessageResponseDto> getCurrentSink() {
        return currentSink;
    }

    public void setCurrentSink(FluxSink<StreamMessageResponseDto> currentSink) {
        this.currentSink = currentSink;
    }
}