package org.dromara.system.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * 会话信息VO
 *
 * @author zhoudashuai
 */
@Data
public class ChatSessionVo {

    /**
     * 会话ID
     */
    private Long chatId;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 会话状态
     */
    private String status;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 对话模式 debug 表示调试模式，chat 表示正常对话
     */
    private String chatModel;
}