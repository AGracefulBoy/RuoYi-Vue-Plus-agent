package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.List;

/**
 * 智能体管理表 sys_agent
 *
 * @author 系统管理员
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_agent", autoResultMap = true)
public class SysAgent extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    @TableId(value = "agent_id")
    private Long agentId;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 智能体描述
     */
    private String agentDesc;

    /**
     * 智能体类型（chat对话型、task任务型、workflow工作流型）
     */
    private String agentType;

    /**
     * 智能体头像URL
     */
    private String avatar;

    /**
     * 智能体提示词
     */
    private String promptContent;

    /**
     * 常见问题（JSON数组）
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
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
    private String model;

    /**
     * 增强回复模型
     */
    private String enhanceModel;

    /**
     * 工具列表（JSON数组，存储工具ID）
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<Long> toolList;

    /**
     * 知识库列表（JSON数组，存储知识库ID）
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<Long> knowledgeBaseList;

    /**
     * 数据库列表（JSON数组，存储数据库配置）
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<Long> databaseList;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

    /**
     * 设置默认值
     */
    public SysAgent() {
        this.status = "0"; // 默认正常状态
        this.conversationMode = "single"; // 默认单轮对话
    }
}
