package org.dromara.system.domain.vo;


import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysAgent;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 智能体管理视图对象 sys_agent
 *
 * @author 系统管理员
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysAgent.class)
public class SysAgentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    @ExcelProperty(value = "智能体ID")
    private Long agentId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 智能体名称
     */
    @ExcelProperty(value = "智能体名称")
    private String agentName;

    /**
     * 智能体描述
     */
    @ExcelProperty(value = "智能体描述")
    private String agentDesc;

    /**
     * 智能体类型（chat对话型、task任务型、workflow工作流型）
     */
    @ExcelProperty(value = "智能体类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_agent_type")
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
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 对话模式（single单轮、multi多轮、context上下文）
     */
    @ExcelProperty(value = "对话模式", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_conversation_mode")
    private String conversationMode;

    /**
     * 主要模型
     */
    @ExcelProperty(value = "主要模型")
    private String model;

    /**
     * 增强回复模型
     */
    @ExcelProperty(value = "增强回复模型")
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
     * 创建部门
     */
    private Long createDept;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建者名称
     */
    @ExcelProperty(value = "创建者名称")
    private String createByName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新者名称
     */
    @ExcelProperty(value = "更新者名称")
    private String updateByName;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
