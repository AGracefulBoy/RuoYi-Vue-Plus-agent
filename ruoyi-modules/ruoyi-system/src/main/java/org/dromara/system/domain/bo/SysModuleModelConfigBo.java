package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 模块模型配置业务对象
 * 用于批量配置模块的模型关联关系和默认模型
 *
 * @author 系统管理员
 */
@Data
public class SysModuleModelConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模块ID
     */
    @NotNull(message = "模块ID不能为空")
    private Long moduleId;

    /**
     * 模型ID列表
     */
    private List<Long> modelIds;

    /**
     * 默认模型ID
     */
    private Long defaultModelId;
}