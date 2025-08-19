package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.InstallPackageRequestBo;
import org.dromara.system.domain.bo.UninstallPackageRequestBo;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.bo.SysToolBasicInfoBo;
import org.dromara.system.domain.bo.ToolDebugRequestBo;
//import org.dromara.system.domain.vo.SysPythonPackageVo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.domain.vo.PythonPackageVo;
import org.dromara.system.domain.vo.ToolPackageVo;
import org.dromara.system.service.ISysToolService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
     * 新增工具管理
     */
    @SaCheckPermission("system:tool:add")
    @Log(title = "工具管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<SysToolVo> add(@Validated(AddGroup.class) @RequestBody SysToolBo bo) {
        if (!toolService.checkToolNameUnique(bo)) {
            return R.fail("新增工具'" + bo.getToolName() + "'失败，工具名称已存在");
        }
        return R.ok(toolService.insertByBo(bo));
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
     * 修改工具基本信息
     * 仅更新工具的基本描述信息，不影响技术配置
     */
    @SaCheckPermission("system:tool:edit")
    @Log(title = "工具基本信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/editBasicInfo")
    public R<Void> editBasicInfo(@Validated(EditGroup.class) @RequestBody SysToolBasicInfoBo bo) {
        // 如果更新工具名称，需要检查唯一性
        if (bo.getToolName() != null) {
            SysToolBo checkBo = new SysToolBo();
            checkBo.setToolId(bo.getToolId());
            checkBo.setToolName(bo.getToolName());
            if (!toolService.checkToolNameUnique(checkBo)) {
                return R.fail("修改工具'" + bo.getToolName() + "'失败，工具名称已存在");
            }
        }
        return toAjax(toolService.updateBasicInfoByBo(bo));
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
     * 安装工具的Python包依赖
     *
     * @param request 安装请求参数（包含工具ID）
     */
    @SaCheckPermission("system:tool:edit")
    @Log(title = "安装Python包", businessType = BusinessType.OTHER)
    @PostMapping("/packages/install")
    public R<String> installToolPackages(@RequestBody @Validated InstallPackageRequestBo request) {
        String result = toolService.installToolPackage(request);
        return R.ok(result);
    }

    /**
     * 卸载Python包
     */
    @SaCheckPermission("system:tool:edit")
    @Log(title = "卸载Python包", businessType = BusinessType.OTHER)
    @PostMapping("/packages/uninstall")
    public R<String> uninstallToolPackages(@RequestBody @Validated UninstallPackageRequestBo request) {
        String result = toolService.uninstallToolPackage(request);
        return R.ok(result);
    }

    /**
     * 查询工具已安装的Python包
     *
     * @param toolId 工具ID
     * @return 已安装的Python包列表
     */
    @SaCheckPermission("system:tool:query")
    @GetMapping("/{toolId}/packages")
    public R<List<ToolPackageVo>> getInstalledPackages(@PathVariable Long toolId) {
        List<ToolPackageVo> packages = toolService.getInstalledPackagesByToolId(toolId);
        return R.ok(packages);
    }

    /**
     * 执行工具脚本（非流式）
     *
     * @param request 执行请求
     */
    @SaCheckPermission("system:tool:execute")
    @Log(title = "执行工具脚本", businessType = BusinessType.OTHER)
    @PostMapping("/execute")
    public R<String> executeScript(@RequestBody @Validated ToolDebugRequestBo request) {
        try {
            String result = toolService.executeToolScript(request);
            return R.ok(result);
        } catch (Exception e) {
            return R.fail("执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行工具脚本（流式）
     *
     * @param request 执行请求
     * @param response HTTP响应对象
     */
    @SaCheckPermission("system:tool:execute")
    @Log(title = "执行工具脚本(流式)", businessType = BusinessType.OTHER)
    @PostMapping(path = "/execute/stream", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public void executeScriptStream(@RequestBody @Validated ToolDebugRequestBo request,
                                    HttpServletResponse response) {
        try {
            toolService.executeToolScriptStream(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("执行失败: " + e.getMessage());
            } catch (IOException ex) {
                // 写入错误响应失败
            }
        }
    }

    /**
     * 搜索Python包
     *
     * @param query 搜索关键词
     * @param pageQuery 分页参数
     * @return Python包信息分页列表
     */
    @SaCheckPermission("system:tool:list")
    @GetMapping("/searchPythonPackages")
    public TableDataInfo<PythonPackageVo> searchPythonPackages(@RequestParam String query, PageQuery pageQuery) {
        return toolService.searchPythonPackages(query, pageQuery);
    }

}
