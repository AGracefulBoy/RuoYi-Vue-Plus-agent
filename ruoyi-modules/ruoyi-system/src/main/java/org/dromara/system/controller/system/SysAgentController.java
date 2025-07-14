package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
import org.dromara.system.domain.bo.SysAgentBo;
import org.dromara.system.domain.vo.SysAgentVo;
import org.dromara.system.service.ISysAgentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体管理
 *
 * @author 系统管理员
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/agent")
public class SysAgentController extends BaseController {

    private final ISysAgentService agentService;

    /**
     * 查询智能体管理列表
     */
    @SaCheckPermission("system:agent:list")
    @GetMapping("/list")
    public TableDataInfo<SysAgentVo> list(SysAgentBo bo, PageQuery pageQuery) {
        return agentService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出智能体管理列表
     */
    @SaCheckPermission("system:agent:export")
    @Log(title = "智能体管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysAgentBo bo, HttpServletResponse response) {
        List<SysAgentVo> list = agentService.queryList(bo);
        ExcelUtil.exportExcel(list, "智能体管理", SysAgentVo.class, response);
    }

    /**
     * 获取智能体管理详细信息
     *
     * @param agentId 智能体ID
     */
    @SaCheckPermission("system:agent:query")
    @GetMapping("/{agentId}")
    public R<SysAgentVo> getInfo(@NotNull(message = "智能体ID不能为空")
                                 @PathVariable Long agentId) {
        return R.ok(agentService.queryById(agentId));
    }

    /**
     * 新增智能体管理
     */
    @SaCheckPermission("system:agent:add")
    @Log(title = "智能体管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysAgentBo bo) {
        return toAjax(agentService.insertByBo(bo));
    }

    /**
     * 修改智能体管理
     */
    @SaCheckPermission("system:agent:edit")
    @Log(title = "智能体管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysAgentBo bo) {
        return toAjax(agentService.updateByBo(bo));
    }

    /**
     * 状态修改
     */
    @SaCheckPermission("system:agent:edit")
    @Log(title = "智能体管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysAgentBo bo) {
        return toAjax(agentService.updateAgentStatus(bo.getAgentId(), bo.getStatus()));
    }

    /**
     * 删除智能体管理
     *
     * @param agentIds 智能体ID串
     */
    @SaCheckPermission("system:agent:remove")
    @Log(title = "智能体管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{agentIds}")
    public R<Void> remove(@NotNull(message = "智能体ID不能为空")
                          @PathVariable Long[] agentIds) {
        return toAjax(agentService.deleteWithValidByIds(List.of(agentIds), true));
    }

    /**
     * 校验智能体名称唯一性
     */
    @GetMapping("/checkAgentNameUnique")
    public R<Boolean> checkAgentNameUnique(SysAgentBo bo) {
        return R.ok(agentService.checkAgentNameUnique(bo));
    }

    /**
     * 根据智能体类型查询智能体列表
     *
     * @param agentType 智能体类型
     */
    @SaCheckPermission("system:agent:query")
    @GetMapping("/type/{agentType}")
    public R<List<SysAgentVo>> getByAgentType(@PathVariable String agentType) {
        return R.ok(agentService.queryByAgentType(agentType));
    }

    /**
     * 根据状态查询智能体列表
     *
     * @param status 状态
     */
    @SaCheckPermission("system:agent:query")
    @GetMapping("/status/{status}")
    public R<List<SysAgentVo>> getByStatus(@PathVariable String status) {
        return R.ok(agentService.queryByStatus(status));
    }

    /**
     * 获取可用的智能体列表（状态为正常的）
     */
    @SaCheckPermission("system:agent:query")
    @GetMapping("/available")
    public R<List<SysAgentVo>> getAvailableAgents() {
        return R.ok(agentService.queryByStatus("0"));
    }
} 