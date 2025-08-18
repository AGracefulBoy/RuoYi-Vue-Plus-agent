package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 工具虚拟环境状态信息VO
 *
 * @author ruoyi
 */
@Data
public class ToolVenvStatusVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 虚拟环境名称
     */
    private String venvName;

    /**
     * 虚拟环境路径
     */
    private String venvPath;

    /**
     * 环境状态 (pending/creating/ready/error)
     */
    private String venvStatus;

    /**
     * Python版本
     */
    private String pythonVersion;

    /**
     * 环境是否存在
     */
    private Boolean exists;

    /**
     * 环境是否可用
     */
    private Boolean available;

    /**
     * 已安装包数量
     */
    private Integer packageCount;

    /**
     * 已安装包列表
     */
    private List<String> installedPackages;

    /**
     * 环境大小（字节）
     */
    private Long diskUsage;

    /**
     * 环境大小（格式化）
     */
    private String diskUsageFormatted;

    /**
     * 环境创建时间
     */
    private Date venvCreateTime;

    /**
     * 环境最后更新时间
     */
    private Date venvLastUpdate;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * pip版本
     */
    private String pipVersion;

    /**
     * 环境健康度评分 (0-100)
     */
    private Integer healthScore;

    /**
     * 健康检查详情
     */
    private String healthDetails;
}