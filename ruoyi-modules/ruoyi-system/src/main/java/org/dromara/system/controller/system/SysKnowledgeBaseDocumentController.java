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
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseDocumentVo;
import org.dromara.system.service.ISysKnowledgeBaseDocumentService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库文档管理
 *
 * @author ruoyi
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/knowledgeBaseDocument")
public class SysKnowledgeBaseDocumentController extends BaseController {

    private final ISysKnowledgeBaseDocumentService knowledgeBaseDocumentService;

    /**
     * 查询知识库文档管理列表
     */
    @SaCheckPermission("system:knowledgeBaseDocument:list")
    @GetMapping("/list")
    public TableDataInfo<SysKnowledgeBaseDocumentVo> list(SysKnowledgeBaseDocumentBo bo, PageQuery pageQuery) {
        return knowledgeBaseDocumentService.queryPageList(bo, pageQuery);
    }

    /**
     * 根据知识库ID查询文档列表
     */
    @SaCheckPermission("system:knowledgeBaseDocument:list")
    @GetMapping("/listByKnowledgeBase/{knowledgeBaseId}")
    public R<List<SysKnowledgeBaseDocumentVo>> listByKnowledgeBase(@NotNull(message = "知识库ID不能为空")
                                                                   @PathVariable Long knowledgeBaseId) {
        List<SysKnowledgeBaseDocumentVo> list = knowledgeBaseDocumentService.queryByKnowledgeBaseId(knowledgeBaseId);
        return R.ok(list);
    }

    /**
     * 导出知识库文档管理列表
     */
    @SaCheckPermission("system:knowledgeBaseDocument:export")
    @Log(title = "知识库文档管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysKnowledgeBaseDocumentBo bo, HttpServletResponse response) {
        List<SysKnowledgeBaseDocumentVo> list = knowledgeBaseDocumentService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库文档管理", SysKnowledgeBaseDocumentVo.class, response);
    }

    /**
     * 获取知识库文档管理详细信息
     */
    @SaCheckPermission("system:knowledgeBaseDocument:query")
    @GetMapping("/{documentId}")
    public R<SysKnowledgeBaseDocumentVo> getInfo(@NotNull(message = "文档ID不能为空")
                                                 @PathVariable Long documentId) {
        return R.ok(knowledgeBaseDocumentService.queryById(documentId));
    }

    /**
     * 批量新增知识库文档管理
     */
    @SaCheckPermission("system:knowledgeBaseDocument:add")
    @Log(title = "知识库文档管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<Void> add(@Validated(AddGroup.class) @RequestBody List<SysKnowledgeBaseDocumentBo> boList) {
        return toAjax(knowledgeBaseDocumentService.insertByBoList(boList));
    }

    /**
     * 修改知识库文档管理
     */
    @SaCheckPermission("system:knowledgeBaseDocument:edit")
    @Log(title = "知识库文档管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/edit")
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysKnowledgeBaseDocumentBo bo) {
        return toAjax(knowledgeBaseDocumentService.updateByBo(bo));
    }

    /**
     * 更新文档处理状态
     */
    @SaCheckPermission("system:knowledgeBaseDocument:edit")
    @Log(title = "知识库文档管理", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus")
    public R<Void> updateStatus(@RequestParam Long documentId, @RequestParam String status) {
        return toAjax(knowledgeBaseDocumentService.updateDocumentStatus(documentId, status));
    }

    /**
     * 删除知识库文档管理
     */
    @SaCheckPermission("system:knowledgeBaseDocument:remove")
    @Log(title = "知识库文档管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @RequestBody List<Long> documentIds) {
        return toAjax(knowledgeBaseDocumentService.deleteWithValidByIds(documentIds, true));
    }

    /**
     * 根据知识库ID删除文档
     */
    @SaCheckPermission("system:knowledgeBaseDocument:remove")
    @Log(title = "知识库文档管理", businessType = BusinessType.DELETE)
    @PostMapping("/deleteByKnowledgeBase/{knowledgeBaseId}")
    public R<Void> deleteByKnowledgeBase(@NotNull(message = "知识库ID不能为空")
                                         @PathVariable Long knowledgeBaseId) {
        return toAjax(knowledgeBaseDocumentService.deleteByKnowledgeBaseId(knowledgeBaseId));
    }


}
