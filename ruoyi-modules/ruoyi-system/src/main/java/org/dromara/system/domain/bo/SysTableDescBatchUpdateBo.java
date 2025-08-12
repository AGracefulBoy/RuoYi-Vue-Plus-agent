package org.dromara.system.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量表描述更新业务对象
 * 用于批量更新多个表的描述和字段描述
 *
 * @author ruoyi
 */
@Data
public class SysTableDescBatchUpdateBo {

    /**
     * 表描述更新列表
     */
    @Valid
    @NotEmpty(message = "表描述更新列表不能为空")
    private List<SysTableDescUpdateBo> tableDescUpdateList;
}