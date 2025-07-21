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
import org.dromara.system.domain.bo.SysColumnMetadataBo;
import org.dromara.system.domain.vo.SysColumnMetadataVo;
import org.dromara.system.service.ISysColumnMetadataService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字段元数据管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/column/metadata")
public class SysColumnMetadataController extends BaseController {

    private final ISysColumnMetadataService columnMetadataService;

    /**
     * 查询字段元数据管理列表
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/list")
    public TableDataInfo<SysColumnMetadataVo> list(SysColumnMetadataBo bo, PageQuery pageQuery) {
        return columnMetadataService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出字段元数据管理列表
     */
    @SaCheckPermission("system:column:metadata:export")
    @Log(title = "字段元数据管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysColumnMetadataBo bo, HttpServletResponse response) {
        List<SysColumnMetadataVo> list = columnMetadataService.queryList(bo);
        ExcelUtil.exportExcel(list, "字段元数据管理", SysColumnMetadataVo.class, response);
    }

    /**
     * 获取字段元数据管理详细信息
     *
     * @param columnMetaId 字段元数据ID
     */
    @SaCheckPermission("system:column:metadata:query")
    @GetMapping("/{columnMetaId}")
    public R<SysColumnMetadataVo> getInfo(@NotNull(message = "字段元数据ID不能为空") @PathVariable Long columnMetaId) {
        return R.ok(columnMetadataService.queryById(columnMetaId));
    }

    /**
     * 新增字段元数据管理
     */
    @SaCheckPermission("system:column:metadata:add")
    @Log(title = "字段元数据管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysColumnMetadataBo bo) {
        return toAjax(columnMetadataService.insertByBo(bo));
    }

    /**
     * 修改字段元数据管理
     */
    @SaCheckPermission("system:column:metadata:edit")
    @Log(title = "字段元数据管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysColumnMetadataBo bo) {
        return toAjax(columnMetadataService.updateByBo(bo));
    }

    /**
     * 删除字段元数据管理
     *
     * @param columnMetaIds 字段元数据ID列表
     */
    @SaCheckPermission("system:column:metadata:remove")
    @Log(title = "字段元数据管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@NotEmpty(message = "字段元数据ID不能为空") @RequestBody Long[] columnMetaIds) {
        return toAjax(columnMetadataService.deleteWithValidByIds(List.of(columnMetaIds)));
    }

    /**
     * 根据表元数据ID查询字段元数据列表
     *
     * @param tableMetaId 表元数据ID
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/table/{tableMetaId}")
    public R<List<SysColumnMetadataVo>> listByTableMetaId(@NotNull(message = "表元数据ID不能为空") @PathVariable Long tableMetaId) {
        return R.ok(columnMetadataService.queryByTableMetaId(tableMetaId));
    }

    /**
     * 根据数据源ID查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/datasource/{datasourceId}")
    public R<List<SysColumnMetadataVo>> listByDatasourceId(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId) {
        return R.ok(columnMetadataService.queryByDatasourceId(datasourceId));
    }

    /**
     * 根据数据源ID、数据库名称和表名称查询字段元数据列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/datasource/{datasourceId}/database/{databaseName}/table/{tableName}")
    public R<List<SysColumnMetadataVo>> listByDatasourceAndTable(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                                 @PathVariable String databaseName,
                                                                 @PathVariable String tableName) {
        return R.ok(columnMetadataService.queryByDatasourceAndTable(datasourceId, databaseName, tableName));
    }

    /**
     * 获取字段元数据详细信息（通过数据源ID、数据库名称、表名称和字段名称）
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     * @param columnName   字段名称
     */
    @SaCheckPermission("system:column:metadata:query")
    @GetMapping("/datasource/{datasourceId}/database/{databaseName}/table/{tableName}/column/{columnName}")
    public R<SysColumnMetadataVo> getColumnInfo(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                @PathVariable String databaseName,
                                                @PathVariable String tableName,
                                                @PathVariable String columnName) {
        return R.ok(columnMetadataService.queryByDatasourceAndTableAndColumn(datasourceId, databaseName, tableName, columnName));
    }

    /**
     * 根据同步状态查询字段元数据列表
     *
     * @param syncStatus 同步状态（0待同步 1同步成功 2同步失败）
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/syncStatus/{syncStatus}")
    public R<List<SysColumnMetadataVo>> listBySyncStatus(@PathVariable String syncStatus) {
        return R.ok(columnMetadataService.queryBySyncStatus(syncStatus));
    }

    /**
     * 查询主键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/primaryKeys/datasource/{datasourceId}/database/{databaseName}/table/{tableName}")
    public R<List<SysColumnMetadataVo>> listPrimaryKeys(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                        @PathVariable String databaseName,
                                                        @PathVariable String tableName) {
        return R.ok(columnMetadataService.queryPrimaryKeys(datasourceId, databaseName, tableName));
    }

    /**
     * 查询外键字段列表
     *
     * @param datasourceId 数据源ID
     * @param databaseName 数据库名称
     * @param tableName    表名称
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/foreignKeys/datasource/{datasourceId}/database/{databaseName}/table/{tableName}")
    public R<List<SysColumnMetadataVo>> listForeignKeys(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                        @PathVariable String databaseName,
                                                        @PathVariable String tableName) {
        return R.ok(columnMetadataService.queryForeignKeys(datasourceId, databaseName, tableName));
    }

    /**
     * 查询敏感字段列表
     *
     * @param datasourceId     数据源ID
     * @param sensitivityLevel 敏感级别
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/sensitive/datasource/{datasourceId}/level/{sensitivityLevel}")
    public R<List<SysColumnMetadataVo>> listSensitiveColumns(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId,
                                                             @PathVariable String sensitivityLevel) {
        return R.ok(columnMetadataService.querySensitiveColumns(datasourceId, sensitivityLevel));
    }

    /**
     * 查询个人身份信息字段列表
     *
     * @param datasourceId 数据源ID
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/pii/datasource/{datasourceId}")
    public R<List<SysColumnMetadataVo>> listPiiColumns(@NotNull(message = "数据源ID不能为空") @PathVariable Long datasourceId) {
        return R.ok(columnMetadataService.queryPiiColumns(datasourceId));
    }

    /**
     * 统计表的字段数量
     *
     * @param tableMetaId 表元数据ID
     */
    @SaCheckPermission("system:column:metadata:list")
    @GetMapping("/count/table/{tableMetaId}")
    public R<Long> countByTableMetaId(@NotNull(message = "表元数据ID不能为空") @PathVariable Long tableMetaId) {
        return R.ok(columnMetadataService.countByTableMetaId(tableMetaId));
    }

    /**
     * 批量新增字段元数据
     */
    @SaCheckPermission("system:column:metadata:add")
    @Log(title = "批量新增字段元数据", businessType = BusinessType.INSERT)
    @PostMapping("/batchInsert")
    public R<Void> batchInsert(@RequestBody List<SysColumnMetadataBo> columnMetadataList) {
        return toAjax(columnMetadataService.batchInsert(columnMetadataList));
    }

    /**
     * 同步表的字段结构信息
     *
     * @param tableMetaId 表元数据ID
     */
    @SaCheckPermission("system:column:metadata:sync")
    @Log(title = "同步字段结构", businessType = BusinessType.UPDATE)
    @PostMapping("/syncTableColumns")
    public R<Void> syncTableColumns(@NotNull(message = "表元数据ID不能为空") @RequestParam Long tableMetaId) {
        return toAjax(columnMetadataService.syncTableColumns(tableMetaId));
    }

    /**
     * 更新字段元数据同步状态
     *
     * @param columnMetaId     字段元数据ID
     * @param syncStatus       同步状态
     * @param syncErrorMessage 同步错误信息
     */
    @SaCheckPermission("system:column:metadata:edit")
    @Log(title = "更新同步状态", businessType = BusinessType.UPDATE)
    @PostMapping("/updateSyncStatus")
    public R<Void> updateSyncStatus(@NotNull(message = "字段元数据ID不能为空") @RequestParam Long columnMetaId,
                                    @RequestParam String syncStatus,
                                    @RequestParam(required = false) String syncErrorMessage) {
        return toAjax(columnMetadataService.updateSyncStatus(columnMetaId, syncStatus, syncErrorMessage));
    }

    /**
     * 更新字段业务信息
     */
    @SaCheckPermission("system:column:metadata:edit")
    @Log(title = "更新字段业务信息", businessType = BusinessType.UPDATE)
    @PostMapping("/updateBusinessInfo")
    public R<Void> updateBusinessInfo(@NotNull(message = "字段元数据ID不能为空") @RequestParam Long columnMetaId,
                                      @RequestParam(required = false) String businessName,
                                      @RequestParam(required = false) String businessDescription,
                                      @RequestParam(required = false) String dataClassification,
                                      @RequestParam(required = false) String sensitivityLevel,
                                      @RequestParam(required = false) String isPii,
                                      @RequestParam(required = false) String maskingRule,
                                      @RequestParam(required = false) String validationRule) {
        return toAjax(columnMetadataService.updateBusinessInfo(columnMetaId, businessName, businessDescription,
                dataClassification, sensitivityLevel, isPii, maskingRule, validationRule));
    }

}