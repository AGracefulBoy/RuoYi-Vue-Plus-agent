package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysAgent;

import java.util.List;

/**
 * 智能体管理业务对象 sys_agent
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysAgent.class, reverseConvertGenerate = false)
public class SysAgentBo extends BaseEntity {

    /**
     * 智能体ID
     */
    @NotNull(message = "智能体ID不能为空", groups = { EditGroup.class })
    private Long agentId;

    /**
     * 智能体名称
     */
    @NotBlank(message = "智能体名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "智能体名称长度不能超过{max}个字符")
    private String agentName;

    /**
     * 智能体描述
     */
    @Size(min = 0, max = 500, message = "智能体描述长度不能超过{max}个字符")
    private String agentDesc;

    /**
     * 智能体类型（chat对话型、task任务型、workflow工作流型）
     */
    @NotBlank(message = "智能体类型不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 50, message = "智能体类型长度不能超过{max}个字符")
    private String agentType;

    /**
     * 智能体头像URL
     */
    @Size(min = 0, max = 500, message = "智能体头像URL长度不能超过{max}个字符")
    private String avatar;

    /**
     * 智能体提示词
     */
    private String promptContent;

    /**
     * 常见问题（JSON数组）
     */
    private List<String> commonQuestions;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 对话模式（single单轮、multi多轮、context上下文）
     */
    private String conversationMode;

    /**
     * 主要模型
     */
    @Size(min = 0, max = 255, message = "主要模型长度不能超过{max}个字符")
    private String model;

    /**
     * 增强回复模型
     */
    @Size(min = 0, max = 255, message = "增强回复模型长度不能超过{max}个字符")
    private String enhanceModel;

    /**
     * 工具列表（JSON数组，存储工具ID）
     */
    private List<Long> toolList;

    /**
     * 知识库列表（JSON数组，存储知识库ID）
     */
    private List<Long> knowledgeBaseList;

    /**
     * 数据库列表（JSON数组，存储数据库配置）
     */
    private List<Long> databaseList;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;
} 