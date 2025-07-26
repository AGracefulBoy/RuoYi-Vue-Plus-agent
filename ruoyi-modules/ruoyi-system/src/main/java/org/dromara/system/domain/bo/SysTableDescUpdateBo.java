package org.dromara.system.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 表描述更新业务对象
 * 用于限制智能体只能更新表描述字段
 *
 * @author ruoyi
 */
@Data
public class SysTableDescUpdateBo {

    /**
     * 表元数据ID
     */
    @NotNull(message = "表元数据ID不能为空")
    private Long tableMetaId;

    /**
     * 表描述
     */
    @Size(max = 1000, message = "表描述长度不能超过{max}个字符")
    private String tableDesc;

    /**
     * 更新字段描述
     */
    @Valid
    private List<SysColumnDescUpdateBo> sysColumnDescUpdateBoList;
}
