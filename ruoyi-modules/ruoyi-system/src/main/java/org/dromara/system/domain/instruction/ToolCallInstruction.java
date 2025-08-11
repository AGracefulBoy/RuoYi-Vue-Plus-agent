package org.dromara.system.domain.instruction;

/**
 * 工具调用指令
 * 封装工具调用的相关信息
 * 
 * @author TaskAgentService
 */
public class ToolCallInstruction {
    private String toolName;
    private Long id;
    // tool 表示工具, knowledge 表示知识库，datasource 表示数据库
    private String type;
    private String parameters;

    public ToolCallInstruction(String toolName, Long id, String type, String parameters) {
        this.toolName = toolName;
        this.id = id;
        this.type = type;
        this.parameters = parameters;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getToolName() {
        return toolName;
    }

    public String getParameters() {
        return parameters;
    }
}