package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Map;

/**
 * 智能体会话分组表 sys_agent_chat_group
 *
 * @author zhoudashuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_agent_chat_group", autoResultMap = true)
public class SysAgentChatGroup extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @TableId(value = "group_id")
    private Long groupId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 父分组ID（支持多级分组）
     */
    private Long parentId;

    /**
     * 分组图标
     */
    private String icon;

    /**
     * 分组颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否默认分组（0否 1是）
     */
    private String isDefault;

    /**
     * 分组元数据
     */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}