package org.dromara.system.domain.bo;

import lombok.Data;

import java.util.Map;

/**
 * Python代码调试请求对象
 *
 * @author ruoyi
 */
@Data
public class PythonDebugRequestBo {

    /**
     * Python代码
     */
    private String code;

    /**
     * 文件路径列表
     */
    private String files;

    /**
     * 要调用的函数名
     */
    private String functionName;

    /**
     * 函数参数（Map对象）
     */
    private Map<String, Object> params;

    /**
     * 是否使用流式响应
     */
    private Boolean stream = false;
}
