package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysModelConfig;

import java.util.List;

/**
 * 模型配置业务对象 sys_model_config
 *
 * @author 系统管理员
 */

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysModelConfig.class, reverseConvertGenerate = false)
public class SysModelConfigBo extends BaseEntity {

    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 模型编码
     */
    @NotBlank(message = "模型编码不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 0, max = 64, message = "模型编码长度不能超过{max}个字符")
    private String modelCode;

    /**
     * 模型厂商（openai、baidu、alibaba、tencent等）
     */
    @NotBlank(message = "模型厂商不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 0, max = 50, message = "模型厂商长度不能超过{max}个字符")
    private String modelProvider;

    /**
     * 模型类型（支持多种类型：chat聊天、embedding嵌入、image图像等）
     */
    @NotNull(message = "模型类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private List<String> modelType;

    /**
     * 模型适配的范围
     */
    @NotNull(message = "模型适配范围不能为空", groups = {AddGroup.class, EditGroup.class})
    private List<String> moduleType;

    /**
     * 模型API地址
     */
    @NotBlank(message = "模型API地址不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 0, max = 500, message = "模型API地址长度不能超过{max}个字符")
    private String baseUrl;

    /**
     * API密钥
     */
    @NotBlank(message = "API密钥不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 0, max = 500, message = "API密钥长度不能超过{max}个字符")
    private String apiKey;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;

} 