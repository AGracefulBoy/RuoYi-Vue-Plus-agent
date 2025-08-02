package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.bo.ToolDebugRequestBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.service.ISysToolService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/tool")
public class SysToolController extends BaseController {

    private final ISysToolService toolService;

    /**
     * 查询工具管理列表（不包含脚本代码）
     */
    @SaCheckPermission("system:tool:list")
    @GetMapping("/list")
    public TableDataInfo<SysToolListVo> list(SysToolBo bo, PageQuery pageQuery) {
        return toolService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出工具管理列表
     */
    @SaCheckPermission("system:tool:export")
    @Log(title = "工具管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysToolBo bo, HttpServletResponse response) {
        List<SysToolListVo> list = toolService.queryList(bo);
        ExcelUtil.exportExcel(list, "工具管理", SysToolListVo.class, response);
    }

    /**
     * 获取工具管理详细信息（包含脚本代码）
     *
     * @param toolId 工具ID
     */
    @SaCheckPermission("system:tool:query")
    @GetMapping("/{toolId}")
    public R<SysToolVo> getInfo(@PathVariable Long toolId) {
        return R.ok(toolService.queryById(toolId));
    }

    /**
     * 根据工具名称查询工具管理
     *
     * @param toolName 工具名称
     */
    @SaCheckPermission("system:tool:query")
    @GetMapping("/name/{toolName}")
    public R<SysToolVo> getByToolName(@PathVariable String toolName) {
        return R.ok(toolService.queryByToolName(toolName));
    }

    /**
     * 根据工具类型查询工具管理列表
     *
     * @param toolType 工具类型
     */
    @SaCheckPermission("system:tool:query")
    @GetMapping("/type/{toolType}")
    public R<List<SysToolVo>> getByToolType(@PathVariable String toolType) {
        return R.ok(toolService.queryByToolType(toolType));
    }

    /**
     * 根据工具状态查询工具管理列表
     *
     * @param toolStatus 工具状态
     */
    @SaCheckPermission("system:tool:query")
    @GetMapping("/status/{toolStatus}")
    public R<List<SysToolVo>> getByToolStatus(@PathVariable String toolStatus) {
        return R.ok(toolService.queryByToolStatus(toolStatus));
    }

    /**
     * 新增工具管理
     */
    @SaCheckPermission("system:tool:add")
    @Log(title = "工具管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysToolBo bo) {
        if (!toolService.checkToolNameUnique(bo)) {
            return R.fail("新增工具'" + bo.getToolName() + "'失败，工具名称已存在");
        }
        return toAjax(toolService.insertByBo(bo));
    }

    /**
     * 修改工具管理
     */
    @SaCheckPermission("system:tool:edit")
    @Log(title = "工具管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysToolBo bo) {
        if (!toolService.checkToolNameUnique(bo)) {
            return R.fail("修改工具'" + bo.getToolName() + "'失败，工具名称已存在");
        }
        return toAjax(toolService.updateByBo(bo));
    }

    /**
     * 更新工具状态
     *
     * @param toolId     工具ID
     * @param toolStatus 工具状态
     */
    @SaCheckPermission("system:tool:edit")
    @Log(title = "工具管理", businessType = BusinessType.UPDATE)
    @PostMapping("/status/{toolId}/{toolStatus}")
    public R<Void> updateToolStatus(@PathVariable Long toolId, @PathVariable String toolStatus) {
        return toAjax(toolService.updateToolStatus(toolId, toolStatus));
    }

    /**
     * 删除工具管理
     *
     * @param toolIds 工具ID串
     */
    @SaCheckPermission("system:tool:remove")
    @Log(title = "工具管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@RequestBody List<Long> toolIds) {
        return toAjax(toolService.deleteWithValidByIds(toolIds, true));
    }

    /**
     * 复制工具管理
     *
     * @param toolId 工具ID
     */
    @SaCheckPermission("system:tool:add")
    @Log(title = "工具管理", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{toolId}")
    public R<Void> copy(@PathVariable Long toolId) {
        if (toolService.copyTool(toolId)) {
            return R.ok("复制工具成功");
        }
        return R.fail("复制工具失败，原工具不存在");
    }

    /**
     * 根据工具ID执行Python代码调试（支持流式和非流式）
     *
     * @param toolId   工具ID
     * @param request  调试请求对象
     * @param response HTTP响应
     */
    @SaCheckPermission("system:tool:debug")
    @Log(title = "工具代码调试", businessType = BusinessType.OTHER)
    @PostMapping("/debug/{toolId}")
    public void debugByToolId(@PathVariable Long toolId,
                              @RequestBody ToolDebugRequestBo request,
                              HttpServletResponse response) {
        request.setToolId(toolId);
        toolService.debugToolCode(request, response);
    }

}
