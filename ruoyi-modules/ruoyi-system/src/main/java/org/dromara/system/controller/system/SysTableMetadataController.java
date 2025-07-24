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
import org.dromara.system.domain.bo.SysTableMetadataBo;
import org.dromara.system.domain.vo.SysTableMetadataVo;
import org.dromara.system.service.ISysTableMetadataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表元数据管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/table/metadata")
public class SysTableMetadataController extends BaseController {

    private final ISysTableMetadataService tableMetadataService;

    /**
     * 查询表元数据管理列表
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/list")
    public TableDataInfo<SysTableMetadataVo> list(SysTableMetadataBo bo, PageQuery pageQuery) {
        return tableMetadataService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出表元数据管理列表
     */
    @SaCheckPermission("system:table:metadata:export")
    @Log(title = "表元数据管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysTableMetadataBo bo, HttpServletResponse response) {
        List<SysTableMetadataVo> list = tableMetadataService.queryList(bo);
        ExcelUtil.exportExcel(list, "表元数据管理", SysTableMetadataVo.class, response);
    }

    /**
     * 获取表元数据管理详细信息
     *
     * @param tableMetaId 表元数据ID
     */
    @SaCheckPermission("system:table:metadata:query")
    @GetMapping("/{tableMetaId}")
    public R<SysTableMetadataVo> getInfo(@NotNull(message = "表元数据ID不能为空") @PathVariable Long tableMetaId) {
        return R.ok(tableMetadataService.queryById(tableMetaId));
    }

    /**
     * 新增表元数据管理
     */
    @SaCheckPermission("system:table:metadata:add")
    @Log(title = "表元数据管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysTableMetadataBo bo) {
        return toAjax(tableMetadataService.insertByBo(bo));
    }

    /**
     * 修改表元数据管理
     */
    @SaCheckPermission("system:table:metadata:edit")
    @Log(title = "表元数据管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysTableMetadataBo bo) {
        return toAjax(tableMetadataService.updateByBo(bo));
    }

    /**
     * 删除表元数据管理
     *
     * @param tableMetaIds 表元数据ID列表
     */
    @SaCheckPermission("system:table:metadata:remove")
    @Log(title = "表元数据管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@NotEmpty(message = "表元数据ID不能为空") @RequestBody Long[] tableMetaIds) {
        return toAjax(tableMetadataService.deleteWithValidByIds(List.of(tableMetaIds)));
    }

    /**
     * 根据数据源ID查询表元数据列表
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/datasource/{datasourceId}")
    public TableDataInfo<SysTableMetadataVo> listByDatasourceId(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                                PageQuery pageQuery) {
        return tableMetadataService.queryPageByDatasourceId(datasourceId, pageQuery);
    }

    /**
     * 根据数据源ID和数据库名称查询表元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/datasource/{datasourceId}/database/{databaseName}")
    public R<List<SysTableMetadataVo>> listByDatasourceAndDatabase(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                                   @PathVariable String databaseName) {
        return R.ok(tableMetadataService.queryByDatasourceIdAndDatabase(datasourceId, databaseName));
    }

    /**
     * 获取表元数据详细信息（通过数据源ID、数据库名称和表名称）
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     */
    @SaCheckPermission("system:table:metadata:query")
    @GetMapping("/datasource/{datasourceId}/database/{databaseName}/table/{tableName}")
    public R<SysTableMetadataVo> getTableInfo(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                               @PathVariable String databaseName,
                                               @PathVariable String tableName) {
        return R.ok(tableMetadataService.queryByDatasourceIdAndDatabaseAndTable(datasourceId, databaseName, tableName));
    }

    /**
     * 根据同步状态查询表元数据列表
     *
     * @param syncStatus 同步状态（0待同步 1同步成功 2同步失败）
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/syncStatus/{syncStatus}")
    public R<List<SysTableMetadataVo>> listBySyncStatus(@PathVariable String syncStatus) {
        return R.ok(tableMetadataService.queryBySyncStatus(syncStatus));
    }

    /**
     * 统计数据源下的表数量
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/count/datasource/{datasourceId}")
    public R<Long> countByDatasourceId(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId) {
        return R.ok(tableMetadataService.countByDatasourceId(datasourceId));
    }

    /**
     * 统计数据库下的表数量
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/count/datasource/{datasourceId}/database/{databaseName}")
    public R<Long> countByDatasourceAndDatabase(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                @PathVariable String databaseName) {
        return R.ok(tableMetadataService.countByDatasourceIdAndDatabase(datasourceId, databaseName));
    }

    /**
     * 查询需要同步的表元数据列表
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:table:metadata:list")
    @GetMapping("/pending/datasource/{datasourceId}")
    public R<List<SysTableMetadataVo>> listPendingSyncTables(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId) {
        return R.ok(tableMetadataService.queryPendingSyncTables(datasourceId));
    }

    /**
     * 同步数据源表结构信息
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:table:metadata:sync")
    @Log(title = "同步表结构", businessType = BusinessType.UPDATE)
    @PostMapping("/syncTableStructure")
    public R<Void> syncTableStructure(@NotNull(message = "数据源ID不能为空") @RequestParam Long datasourceId) {
        return toAjax(tableMetadataService.syncTableStructure(datasourceId));
    }

    /**
     * 同步单个表的结构信息
     *
     * @param tableMetaId 表元数据ID
     */
    @SaCheckPermission("system:table:metadata:sync")
    @Log(title = "同步表结构", businessType = BusinessType.UPDATE)
    @PostMapping("/syncSingleTable")
    public R<Void> syncSingleTable(@NotNull(message = "表元数据ID不能为空") @RequestParam Long tableMetaId) {
        return toAjax(tableMetadataService.syncSingleTable(tableMetaId));
    }

    /**
     * 更新表元数据同步状态
     *
     * @param tableMetaId      表元数据ID
     * @param syncStatus       同步状态
     * @param syncErrorMessage 同步错误信息
     */
    @SaCheckPermission("system:table:metadata:edit")
    @Log(title = "更新同步状态", businessType = BusinessType.UPDATE)
    @PostMapping("/updateSyncStatus")
    public R<Void> updateSyncStatus(@NotNull(message = "表元数据ID不能为空") @RequestParam Long tableMetaId,
                                    @RequestParam String syncStatus,
                                    @RequestParam(required = false) String syncErrorMessage) {
        return toAjax(tableMetadataService.updateSyncStatus(tableMetaId, syncStatus, syncErrorMessage));
    }

}