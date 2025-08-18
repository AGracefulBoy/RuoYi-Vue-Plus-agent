package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Python包信息视图对象
 *
 * @author ruoyi
 */
@Data
public class PythonPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 包版本号
     */
    private String packageVersion;

    /**
     * 包描述
     */
    private String packageDesc;
}