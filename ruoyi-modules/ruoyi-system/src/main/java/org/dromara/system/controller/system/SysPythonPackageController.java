package org.dromara.system.controller.system;

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
import org.dromara.system.domain.bo.SysPythonPackageBo;
import org.dromara.system.domain.vo.SysPythonPackageVo;
import org.dromara.system.service.ISysPythonPackageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Python包管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/pythonPackage")
public class SysPythonPackageController extends BaseController {

    private final ISysPythonPackageService pythonPackageService;

    /**
     * 查询Python包管理列表
     */
    @SaCheckPermission("system:pythonPackage:list")
    @GetMapping("/list")
    public TableDataInfo<SysPythonPackageVo> list(SysPythonPackageBo bo, PageQuery pageQuery) {
        return pythonPackageService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出Python包管理列表
     */
    @SaCheckPermission("system:pythonPackage:export")
    @Log(title = "Python包管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysPythonPackageBo bo, HttpServletResponse response) {
        List<SysPythonPackageVo> list = pythonPackageService.queryList(bo);
        ExcelUtil.exportExcel(list, "Python包管理", SysPythonPackageVo.class, response);
    }

    /**
     * 获取Python包管理详细信息
     *
     * @param packageId 包ID
     */
    @SaCheckPermission("system:pythonPackage:query")
    @GetMapping("/{packageId}")
    public R<SysPythonPackageVo> getInfo(@PathVariable Long packageId) {
        return R.ok(pythonPackageService.queryById(packageId));
    }

    /**
     * 根据包名查询Python包管理
     *
     * @param packageName 包名
     */
    @SaCheckPermission("system:pythonPackage:query")
    @GetMapping("/name/{packageName}")
    public R<SysPythonPackageVo> getByPackageName(@PathVariable String packageName) {
        return R.ok(pythonPackageService.queryByPackageName(packageName));
    }

    /**
     * 根据安装状态查询Python包管理列表
     *
     * @param isInstalled 是否已安装
     */
    @SaCheckPermission("system:pythonPackage:query")
    @GetMapping("/installStatus/{isInstalled}")
    public R<List<SysPythonPackageVo>> getByInstallStatus(@PathVariable String isInstalled) {
        return R.ok(pythonPackageService.queryByInstallStatus(isInstalled));
    }

    /**
     * 批量新增Python包管理
     */
    @SaCheckPermission("system:pythonPackage:add")
    @Log(title = "Python包管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody List<SysPythonPackageBo> boList) {
        // 校验包名唯一性
        for (SysPythonPackageBo bo : boList) {
            if (!pythonPackageService.checkPackageNameUnique(bo)) {
                return R.fail("新增Python包'" + bo.getPackageName() + "'失败，包名已存在");
            }
        }
        return toAjax(pythonPackageService.insertBatchByBo(boList));
    }

    /**
     * 修改Python包管理
     */
    @SaCheckPermission("system:pythonPackage:edit")
    @Log(title = "Python包管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysPythonPackageBo bo) {
        if (!pythonPackageService.checkPackageNameUnique(bo)) {
            return R.fail("修改Python包'" + bo.getPackageName() + "'失败，包名已存在");
        }
        return toAjax(pythonPackageService.updateByBo(bo));
    }

    /**
     * 更新包安装状态
     *
     * @param packageId   包ID
     * @param isInstalled 是否已安装
     */
    @SaCheckPermission("system:pythonPackage:edit")
    @Log(title = "Python包管理", businessType = BusinessType.UPDATE)
    @PostMapping("/installStatus/{packageId}/{isInstalled}")
    public R<Void> updateInstallStatus(@PathVariable Long packageId, @PathVariable String isInstalled) {
        return toAjax(pythonPackageService.updateInstallStatus(packageId, isInstalled));
    }

    /**
     * 删除Python包管理
     *
     * @param packageIds 包ID数组
     */
    @SaCheckPermission("system:pythonPackage:remove")
    @Log(title = "Python包管理", businessType = BusinessType.DELETE)
    @PostMapping("/{packageIds}")
    public R<Void> remove(@RequestBody List<Long> packageIds) {
        return toAjax(pythonPackageService.deleteWithValidByIds(packageIds, true));
    }
}
