package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import org.apache.ibatis.type.StringTypeHandler;
import org.dromara.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 模型配置表 sys_model_config
 *
 * @author 系统管理员
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_model_config",autoResultMap = true)
public class SysModelConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    @TableId(value = "model_id")
    private Long modelId;

    /**
     * 模型编码
     */
    private String modelCode;

    /**
     * 模型厂商（openai、baidu、alibaba、tencent等）
     */
    private String modelProvider;

    /**
     * 模型类型（支持多种类型：chat聊天、embedding嵌入、image图像等）
     */
    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<String> modelType;

    /**
     * 模型API地址
     */
    private String baseUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
