package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 文档任务对象 document_task
 */
@Data
@TableName("document_task")
public class DocumentTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 状态
     */
    private String status;

    /**
     * 原始URL
     */
    private String originalUrl;

    /**
     * 处理后URL
     */
    private String processedUrl;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 参数
     */
    private String parameters;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;
}