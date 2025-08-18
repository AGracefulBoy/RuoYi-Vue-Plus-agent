package org.dromara.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工具包视图对象
 *
 * @author ruoyi
 */
@Data
public class ToolPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 包ID（自增主键）
     */
    private Long packageId;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 包版本号
     */
    private String packageVersion;

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