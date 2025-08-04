package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.dromara.system.domain.bo.SysModelConfigBo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.ISysModelConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型配置
 *
 * @author 系统管理员
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/modelConfig")
public class SysModelConfigController extends BaseController {

    private final ISysModelConfigService modelConfigService;

    /**
     * 查询模型配置列表
     */
    @SaCheckPermission("system:modelConfig:list")
    @GetMapping("/list")
    public TableDataInfo<SysModelConfigVo> list(SysModelConfigBo bo, PageQuery pageQuery) {
        return modelConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取模型配置详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:modelConfig:query")
    @GetMapping("/{id}")
    public R<SysModelConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(modelConfigService.queryById(id));
    }

    /**
     * 根据模型编码查询模型配置
     *
     * @param modelCode 模型编码
     */
    @SaCheckPermission("system:modelConfig:query")
    @GetMapping("/code/{modelCode}")
    public R<SysModelConfigVo> getByModelCode(@PathVariable String modelCode) {
        return R.ok(modelConfigService.queryByModelCode(modelCode));
    }

    /**
     * 根据模型厂商查询模型配置列表
     *
     * @param modelProvider 模型厂商
     */
    @SaCheckPermission("system:modelConfig:query")
    @GetMapping("/provider/{modelProvider}")
    public R<List<SysModelConfigVo>> getByModelProvider(@PathVariable String modelProvider) {
        return R.ok(modelConfigService.queryByModelProvider(modelProvider));
    }

    /**
     * 根据模型类型查询模型配置列表
     *
     * @param modelType 模型类型
     */
    @SaCheckPermission("system:modelConfig:query")
    @GetMapping("/type/{modelType}")
    public R<List<SysModelConfigVo>> getByModelType(@PathVariable String modelType) {
        return R.ok(modelConfigService.queryByModelType(modelType));
    }

    /**
     * 根据模型适配范围查询模型配置列表
     *
     * @param moduleType 模型适配范围
     */
    @SaCheckPermission("system:modelConfig:query")
    @GetMapping("/module/{moduleType}")
    public R<List<SysModelConfigVo>> getByModuleType(@PathVariable String moduleType) {
        return R.ok(modelConfigService.queryByModuleType(moduleType));
    }

    /**
     * 新增模型配置
     */
    @SaCheckPermission("system:modelConfig:add")
    @Log(title = "新增模型配置", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysModelConfigBo bo) {
        return toAjax(modelConfigService.insertByBo(bo));
    }

    /**
     * 修改模型配置
     */
    @SaCheckPermission("system:modelConfig:edit")
    @Log(title = "编辑模型配置", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysModelConfigBo bo) {
        return toAjax(modelConfigService.updateByBo(bo));
    }

    /**
     * 删除模型配置
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:modelConfig:remove")
    @Log(title = "模型配置", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@RequestBody List<Long> ids) {
        return toAjax(modelConfigService.deleteWithValidByIds(ids, true));
    }

}
