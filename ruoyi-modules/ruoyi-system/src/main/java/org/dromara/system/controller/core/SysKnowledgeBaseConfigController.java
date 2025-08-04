package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysKnowledgeBaseConfigBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseConfigVo;
import org.dromara.system.service.ISysKnowledgeBaseConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库默认配置
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/knowledgeBaseConfig")
public class SysKnowledgeBaseConfigController extends BaseController {

    private final ISysKnowledgeBaseConfigService knowledgeBaseConfigService;

    /**
     * 查询知识库默认配置列表
     */
    @SaCheckPermission("system:knowledgeBaseConfig:list")
    @GetMapping("/list")
    public TableDataInfo<SysKnowledgeBaseConfigVo> list(SysKnowledgeBaseConfigBo bo, PageQuery pageQuery) {
        return knowledgeBaseConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 获取知识库默认配置详细信息
     *
     * @param knowledgeBaseConfigId 主键
     */
    @SaCheckPermission("system:knowledgeBaseConfig:query")
    @GetMapping("/{knowledgeBaseConfigId}")
    public R<SysKnowledgeBaseConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                               @PathVariable Long knowledgeBaseConfigId) {
        return R.ok(knowledgeBaseConfigService.queryById(knowledgeBaseConfigId));
    }

    /**
     * 获取默认配置
     */
    @SaCheckPermission("system:knowledgeBaseConfig:query")
    @GetMapping("/default")
    public R<SysKnowledgeBaseConfigVo> getDefaultConfig() {
        return R.ok(knowledgeBaseConfigService.getDefaultConfig());
    }

    /**
     * 新增知识库默认配置
     */
    @SaCheckPermission("system:knowledgeBaseConfig:add")
    @Log(title = "知识库默认配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysKnowledgeBaseConfigBo bo) {
        return toAjax(knowledgeBaseConfigService.insertByBo(bo));
    }

    /**
     * 修改知识库默认配置
     */
    @SaCheckPermission("system:knowledgeBaseConfig:edit")
    @Log(title = "知识库默认配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysKnowledgeBaseConfigBo bo) {
        return toAjax(knowledgeBaseConfigService.updateByBo(bo));
    }

    /**
     * 删除知识库默认配置
     *
     * @param knowledgeBaseConfigIds 主键串
     */
    @SaCheckPermission("system:knowledgeBaseConfig:remove")
    @Log(title = "知识库默认配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{knowledgeBaseConfigIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] knowledgeBaseConfigIds) {
        return toAjax(knowledgeBaseConfigService.deleteWithValidByIds(List.of(knowledgeBaseConfigIds), true));
    }

}
