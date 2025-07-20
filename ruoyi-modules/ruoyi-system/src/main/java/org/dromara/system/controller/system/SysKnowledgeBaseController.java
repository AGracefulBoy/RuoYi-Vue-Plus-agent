package org.dromara.system.controller.system;

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
import org.dromara.system.domain.bo.SysKnowledgeBaseBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.service.ISysKnowledgeBaseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/knowledgeBase")
public class SysKnowledgeBaseController extends BaseController {

    private final ISysKnowledgeBaseService knowledgeBaseService;

    /**
     * 查询知识库管理列表
     */
    @SaCheckPermission("system:knowledgeBase:list")
    @GetMapping("/list")
    public TableDataInfo<SysKnowledgeBaseVo> list(SysKnowledgeBaseBo bo, PageQuery pageQuery) {
        return knowledgeBaseService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出知识库管理列表
     */
    @SaCheckPermission("system:knowledgeBase:export")
    @Log(title = "知识库管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysKnowledgeBaseBo bo, HttpServletResponse response) {
        List<SysKnowledgeBaseVo> list = knowledgeBaseService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库管理", SysKnowledgeBaseVo.class, response);
    }

    /**
     * 获取知识库管理详细信息
     *
     * @param knowledgeBaseId 知识库ID
     */
    @SaCheckPermission("system:knowledgeBase:query")
    @GetMapping("/{knowledgeBaseId}")
    public R<SysKnowledgeBaseVo> getInfo(@NotNull(message = "知识库ID不能为空")
                                         @PathVariable Long knowledgeBaseId) {
        return R.ok(knowledgeBaseService.queryById(knowledgeBaseId));
    }

    /**
     * 根据知识库名称查询知识库管理
     *
     * @param name 知识库名称
     */
    @SaCheckPermission("system:knowledgeBase:query")
    @GetMapping("/name/{name}")
    public R<SysKnowledgeBaseVo> getByName(@PathVariable String name) {
        return R.ok(knowledgeBaseService.queryByName(name));
    }

    /**
     * 新增知识库管理
     */
    @SaCheckPermission("system:knowledgeBase:add")
    @Log(title = "知识库管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysKnowledgeBaseBo bo) {
        return toAjax(knowledgeBaseService.insertByBo(bo));
    }

    /**
     * 修改知识库管理
     */
    @SaCheckPermission("system:knowledgeBase:edit")
    @Log(title = "知识库管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysKnowledgeBaseBo bo) {
        return toAjax(knowledgeBaseService.updateByBo(bo));
    }

    /**
     * 状态修改
     */
    @SaCheckPermission("system:knowledgeBase:edit")
    @Log(title = "知识库管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysKnowledgeBaseBo bo) {
        return toAjax(knowledgeBaseService.updateKnowledgeBaseStatus(bo.getKnowledgeBaseId(), bo.getStatus()));
    }

    /**
     * 删除知识库管理
     *
     * @param knowledgeBaseIds 知识库ID串
     */
    @SaCheckPermission("system:knowledgeBase:remove")
    @Log(title = "知识库管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @RequestBody List<Long> knowledgeBaseIds) {
        return toAjax(knowledgeBaseService.deleteWithValidByIds(knowledgeBaseIds, true));
    }

    /**
     * 校验知识库名称
     */
    @PostMapping("/checkKnowledgeBaseName")
    public R<Boolean> checkKnowledgeBaseName(@RequestBody SysKnowledgeBaseBo bo) {
        return R.ok(knowledgeBaseService.checkNameUnique(bo));
    }

}
