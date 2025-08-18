package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * 工具虚拟环境操作日志实体
 *
 * @author ruoyi
 */
@Data
@TableName("sys_tool_venv_log")
public class SysToolVenvLog extends TenantEntity {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 操作类型(create/update/delete/install/rebuild)
     */
    private String operation;

    /**
     * 操作状态(success/failed/running)
     */
    private String status;

    /**
     * 操作消息
     */
    private String message;

    /**
     * 执行的命令
     */
    private String command;

    /**
     * 执行结果
     */
    private String result;

    /**
     * 操作人ID
     */
    private Long operator;

    /**
     * 操作时间
     */
    private Date createTime;

    /**
     * 执行耗时(秒)
     */
    private Integer duration;
}
