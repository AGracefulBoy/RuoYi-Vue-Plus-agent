package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 智能体会话详情视图对象
 *
 * @author system
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysAgentChat.class)
public class SysAgentChatDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @ExcelProperty(value = "会话ID")
    private Long chatId;

    /**
     * 会话ID（用于前端标识，继续会话时必填）
     */
    @ExcelProperty(value = "会话标识")
    private String conversationId;

    /**
     * 智能体ID
     */
    @ExcelProperty(value = "智能体ID")
    private Long agentId;

    /**
     * 用户ID
     */
    @ExcelProperty(value = "用户ID")
    private Long userId;

    /**
     * 分组ID
     */
    @ExcelProperty(value = "分组ID")
    private Long groupId;

    /**
     * 会话标题
     */
    @ExcelProperty(value = "会话标题")
    private String chatTitle;

    /**
     * 会话状态（active活跃、archived归档、deleted删除）
     */
    @ExcelProperty(value = "会话状态")
    private String status;

    /**
     * 消息总数
     */
    @ExcelProperty(value = "消息总数")
    private Integer messageCount;

    /**
     * 总token消耗
     */
    @ExcelProperty(value = "总Token消耗")
    private Integer totalTokens;

    /**
     * 输入token消耗
     */
    @ExcelProperty(value = "输入Token消耗")
    private Integer inputTokens;

    /**
     * 输出token消耗
     */
    @ExcelProperty(value = "输出Token消耗")
    private Integer outputTokens;

    /**
     * 对话模式（debug 表示调试模式，chat 表示正常对话）
     */
    @ExcelProperty(value = "对话模式")
    private String chatModel;

    /**
     * 最后活跃时间
     */
    @ExcelProperty(value = "最后活跃时间")
    private Date lastActiveTime;

    /**
     * 会话开始时间
     */
    @ExcelProperty(value = "会话开始时间")
    private Date startTime;

    /**
     * 会话结束时间
     */
    @ExcelProperty(value = "会话结束时间")
    private Date endTime;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 会话消息列表
     */
    @TableField(exist = false)
    private List<SysAgentChatMessage> messages;

}