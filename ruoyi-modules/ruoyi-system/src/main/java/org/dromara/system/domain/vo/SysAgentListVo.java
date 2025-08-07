package org.dromara.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 智能体管理列表视图对象 sys_agent
 * 仅包含列表展示所需的关键字段
 *
 * @author 系统管理员
 */
@Data
public class SysAgentListVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
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
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 创建者
     */
    private Long createBy;


    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 创建者名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "createBy")
    private String createByName;



    /**
     * 更新者名称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "updateBy")
    private String updateByName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
