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
     * 会话UUID
     */
    private String chatUuid;

    /**
     * 智能体ID
     */
    private Long agentId;

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
}