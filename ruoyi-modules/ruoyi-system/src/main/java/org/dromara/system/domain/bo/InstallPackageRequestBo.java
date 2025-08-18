package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Python包安装请求对象
 *
 * @author ruoyi
 */
@Data
public class InstallPackageRequestBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    @NotNull(message = "工具ID不能为空")
    private Long toolId;

    /**
     * Python包名
     */
    @NotBlank(message = "包名不能为空")
    @Size(min = 1, max = 100, message = "包名长度不能超过{max}个字符")
    private String packageName;

    /**
     * 包版本号（可选，默认latest）
     */
    @Size(max = 50, message = "版本号长度不能超过{max}个字符")
    private String packageVersion;
}
