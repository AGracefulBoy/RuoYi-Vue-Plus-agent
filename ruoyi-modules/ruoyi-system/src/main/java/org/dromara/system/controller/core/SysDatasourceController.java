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
import org.dromara.system.domain.bo.SysDatasourceBo;
import org.dromara.system.domain.vo.SysDatasourceListVo;
import org.dromara.system.domain.vo.SysDatasourceVo;
import org.dromara.system.service.ISysDatasourceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/datasource")
public class SysDatasourceController extends BaseController {

    private final ISysDatasourceService datasourceService;

    /**
     * 查询数据源管理列表
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/list")
    public TableDataInfo<SysDatasourceListVo> list(SysDatasourceBo bo, PageQuery pageQuery) {
        return datasourceService.queryPageListForList(bo, pageQuery);
    }

    /**
     * 导出数据源管理列表
     */
    @SaCheckPermission("system:datasource:export")
    @Log(title = "数据源管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysDatasourceBo bo, HttpServletResponse response) {
        List<SysDatasourceVo> list = datasourceService.queryList(bo);
        ExcelUtil.exportExcel(list, "数据源管理", SysDatasourceVo.class, response);
    }

    /**
     * 获取数据源管理详细信息
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:datasource:query")
    @GetMapping("/{datasourceId}")
    public R<SysDatasourceVo> getInfo(@NotNull(message = "数据源ID不能为空")
                                      @PathVariable Long datasourceId) {
        return R.ok(datasourceService.queryById(datasourceId));
    }

    /**
     * 新增数据源管理
     */
    @SaCheckPermission("system:datasource:add")
    @Log(title = "数据源管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysDatasourceBo bo) {
        // 校验数据源类型相关字段
        String validateResult = datasourceService.validateDatasourceFields(bo);
        if (validateResult != null) {
            return R.fail("新增数据源失败，" + validateResult);
        }

        return toAjax(datasourceService.insertByBo(bo));
    }

    /**
     * 修改数据源管理
     */
    @SaCheckPermission("system:datasource:edit")
    @Log(title = "数据源管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysDatasourceBo bo) {
        // 校验数据源名称唯一性
        if (!datasourceService.checkDatasourceNameUnique(bo)) {
            return R.fail("修改数据源'" + bo.getDatasourceName() + "'失败，数据源名称已存在");
        }

        // 校验数据源类型相关字段
        String validateResult = datasourceService.validateDatasourceFields(bo);
        if (validateResult != null) {
            return R.fail("修改数据源失败，" + validateResult);
        }

        return toAjax(datasourceService.updateByBo(bo));
    }

    /**
     * 删除数据源管理
     *
     * @param datasourceIds 数据源ID数组
     */
    @SaCheckPermission("system:datasource:remove")
    @Log(title = "数据源管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@NotEmpty(message = "数据源ID不能为空")
                          @RequestBody List<Long> datasourceIds) {
        return toAjax(datasourceService.deleteWithValidByIds(datasourceIds, true));
    }

    /**
     * 测试数据源连接
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:datasource:test")
    @Log(title = "数据源连接测试", businessType = BusinessType.OTHER)
    @PostMapping("/test/{datasourceId}")
    public R<Void> testConnection(@NotNull(message = "数据源ID不能为空")
                                  @PathVariable Long datasourceId) {
        try {
            boolean isConnected = datasourceService.testConnection(datasourceId);
            if (isConnected) {
                return R.ok("连接测试成功");
            } else {
                return R.fail("连接测试失败，请检查数据源配置");
            }
        } catch (Exception e) {
            return R.fail("连接测试异常：" + e.getMessage());
        }
    }

    /**
     * 测试数据源连接（使用传入的配置）
     */
    @SaCheckPermission("system:datasource:test")
    @Log(title = "数据源连接测试", businessType = BusinessType.OTHER)
    @PostMapping("/testConfig")
    public R<Void> testConnectionByConfig(@Validated @RequestBody SysDatasourceBo bo) {
        try {
            // 校验数据源类型相关字段
            String validateResult = datasourceService.validateDatasourceFields(bo);
            if (validateResult != null) {
                return R.fail("连接测试失败，" + validateResult);
            }

            boolean isConnected = datasourceService.testConnectionByConfig(bo);
            if (isConnected) {
                return R.ok("连接测试成功");
            } else {
                return R.fail("连接测试失败，请检查数据源配置");
            }
        } catch (Exception e) {
            return R.fail("连接测试异常：" + e.getMessage());
        }
    }

    /**
     * 设置默认数据源
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:datasource:edit")
    @Log(title = "设置默认数据源", businessType = BusinessType.UPDATE)
    @PostMapping("/setDefault/{datasourceId}")
    public R<Void> setDefaultDatasource(@NotNull(message = "数据源ID不能为空")
                                        @PathVariable Long datasourceId) {
        return toAjax(datasourceService.setDefaultDatasource(datasourceId));
    }

    /**
     * 根据数据源类型查询数据源列表
     *
     * @param datasourceType 数据源类型
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/listByType/{datasourceType}")
    public R<List<SysDatasourceVo>> listByType(@PathVariable String datasourceType) {
        List<SysDatasourceVo> list = datasourceService.queryByDatasourceType(datasourceType);
        return R.ok(list);
    }

    /**
     * 根据数据库类型查询数据源列表
     *
     * @param databaseType 数据库类型
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/listByDbType/{databaseType}")
    public R<List<SysDatasourceVo>> listByDatabaseType(@PathVariable String databaseType) {
        List<SysDatasourceVo> list = datasourceService.queryByDatabaseType(databaseType);
        return R.ok(list);
    }

    /**
     * 查询默认数据源
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/default")
    public R<SysDatasourceVo> getDefaultDatasource() {
        SysDatasourceVo datasource = datasourceService.queryDefaultDatasource();
        return R.ok(datasource);
    }

    /**
     * 查询连接成功的数据源
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/connected")
    public R<List<SysDatasourceVo>> getConnectedDatasources() {
        List<SysDatasourceVo> list = datasourceService.queryConnectedDatasources();
        return R.ok(list);
    }

    /**
     * 统计不同类型数据源数量
     *
     * @param datasourceType 数据源类型
     */
    @SaCheckPermission("system:datasource:list")
    @GetMapping("/count/{datasourceType}")
    public R<Long> countByDatasourceType(@PathVariable String datasourceType) {
        long count = datasourceService.countByDatasourceType(datasourceType);
        return R.ok(count);
    }

    /**
     * 校验数据源名称是否唯一
     */
    @GetMapping("/checkDatasourceNameUnique")
    public R<Boolean> checkDatasourceNameUnique(SysDatasourceBo datasource) {
        return R.ok(datasourceService.checkDatasourceNameUnique(datasource));
    }

}
