package org.dromara.system.domain.context;

import org.dromara.system.domain.vo.SysModelConfigVo;

/**
 * 模型配置上下文
 * 用于封装主模型和增强模型的配置信息
 * 
 * @author TaskAgentService
 */
public class ModelConfigContext {
    private final SysModelConfigVo mainModel;
    private final SysModelConfigVo enhanceModel;

    public ModelConfigContext(SysModelConfigVo mainModel, SysModelConfigVo enhanceModel) {
        this.mainModel = mainModel;
        this.enhanceModel = enhanceModel;
    }

    public SysModelConfigVo getMainModel() {
        return mainModel;
    }

    public SysModelConfigVo getEnhanceModel() {
        return enhanceModel;
    }
}