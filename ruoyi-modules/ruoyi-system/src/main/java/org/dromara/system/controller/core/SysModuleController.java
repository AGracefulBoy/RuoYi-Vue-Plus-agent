package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ArrayUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysModuleBo;
import org.dromara.system.domain.bo.SysModuleModelConfigBo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.domain.vo.SysModuleVo;
import org.dromara.system.service.ISysModuleModelService;
import org.dromara.system.service.ISysModuleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统模块管理
 *
 * @author 系统管理员
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/module")
public class SysModuleController extends BaseController {

    private final ISysModuleService moduleService;
    private final ISysModuleModelService moduleModelService;

    /**
     * 查询模块列表
     */
    @SaCheckPermission("system:module:list")
    @GetMapping("/list")
    public TableDataInfo<SysModuleVo> list(SysModuleBo bo, PageQuery pageQuery) {
        return moduleService.queryPageList(bo, pageQuery);
    }

    /**
     * 查询所有模块列表（不分页）
     */
    @SaCheckPermission("system:module:query")
    @GetMapping("/listAll")
    public R<List<SysModuleVo>> listAll(SysModuleBo bo) {
        return R.ok(moduleService.queryList(bo));
    }

    /**
     * 根据模块ID查询关联的模型列表
     *
     * @param moduleId 模块ID
     */
    @SaCheckPermission("system:module:query")
    @GetMapping("/{moduleId}/models")
    public R<List<SysModelConfigVo>> getModelsByModuleId(@PathVariable Long moduleId) {
        return R.ok(moduleService.queryModelsByModuleId(moduleId));
    }

    /**
     * 根据模块编码查询关联的模型列表
     *
     * @param moduleCode 模块编码
     */
    @SaCheckPermission("system:module:query")
    @GetMapping("/code/{moduleCode}/models")
    public R<List<SysModelConfigVo>> getModelsByModuleCode(@PathVariable String moduleCode) {
        return R.ok(moduleService.queryModelsByModuleCode(moduleCode));
    }

    /**
     * 配置模块的模型关联关系
     * 一次性完成模型的绑定、解绑和默认模型设置
     *
     * @param configBo 配置参数
     */
    @SaCheckPermission("system:module:edit")
    @Log(title = "模块管理", businessType = BusinessType.UPDATE)
    @PostMapping("/models/config")
    public R<Void> configModuleModels(@Validated @RequestBody SysModuleModelConfigBo configBo) {
        // 设置模块ID到配置对象中
        return toAjax(moduleModelService.configModuleModels(configBo));
    }

    /**
     * 绑定模块与模型关系（保留以兼容旧版本）
     *
     * @deprecated 请使用 configModuleModels 接口
     */
    @Deprecated
    @SaCheckPermission("system:module:edit")
    @Log(title = "模块管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{moduleId}/bind")
    public R<Void> bindModels(@PathVariable Long moduleId, @RequestBody List<Long> modelIds) {
        return toAjax(moduleModelService.bindModuleModels(moduleId, modelIds));
    }

    /**
     * 解绑模块与模型关系（保留以兼容旧版本）
     *
     * @deprecated 请使用 configModuleModels 接口
     */
    @Deprecated
    @SaCheckPermission("system:module:edit")
    @Log(title = "模块管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{moduleId}/unbind")
    public R<Void> unbindModels(@PathVariable Long moduleId, @RequestBody List<Long> modelIds) {
        return toAjax(moduleModelService.unbindModuleModels(moduleId, modelIds));
    }

    /**
     * 设置模块的默认模型（保留以兼容旧版本）
     *
     * @param moduleId 模块ID
     * @param modelId 模型ID
     * @deprecated 请使用 configModuleModels 接口
     */
    @Deprecated
    @SaCheckPermission("system:module:edit")
    @Log(title = "模块管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{moduleId}/default/{modelId}")
    public R<Void> setDefaultModel(@PathVariable Long moduleId, @PathVariable Long modelId) {
        return toAjax(moduleModelService.setDefaultModel(moduleId, modelId));
    }
}
