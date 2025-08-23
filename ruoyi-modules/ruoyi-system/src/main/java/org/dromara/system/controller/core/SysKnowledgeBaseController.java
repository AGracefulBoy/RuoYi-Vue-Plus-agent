package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import cn.hutool.json.JSONUtil;
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
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.dto.KnowledgeSearchRequest;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.domain.vo.SysKnowledgeBaseConfigVo;
import org.dromara.system.domain.vo.SysModuleVo;
import org.dromara.system.service.IElasticsearchDocumentService;
import org.dromara.system.service.ISysKnowledgeBaseService;
import org.dromara.system.service.ISysModuleService;
import org.dromara.system.service.ISysModuleModelService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    private final IElasticsearchDocumentService elasticsearchDocumentService;
    private final ISysModuleService sysModuleService;
    private final ISysModuleModelService sysModuleModelService;

    /**
     * 查询知识库管理列表
     */
    @SaCheckPermission("system:knowledgeBase:list")
    @GetMapping("/list")
    public TableDataInfo<SysKnowledgeBaseVo> list(SysKnowledgeBaseBo bo, PageQuery pageQuery) {
        return knowledgeBaseService.queryPageList(bo, pageQuery);
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
     * 新增知识库管理
     */
    @SaCheckPermission("system:knowledgeBase:add")
    @Log(title = "知识库管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/add")
    public R<SysKnowledgeBaseVo> add(@Validated(AddGroup.class) @RequestBody SysKnowledgeBaseBo bo) {
        return R.ok(knowledgeBaseService.insertByBo(bo));
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

    /**
     * 知识库检索
     *
     * @param request 搜索请求参数
     * @return 搜索结果列表
     */
    @SaCheckPermission("system:knowledgeBase:search")
    @Log(title = "知识库检索", businessType = BusinessType.OTHER)
    @PostMapping("/search")
    public R<List<HitSourceDTO>> search(@Validated @RequestBody KnowledgeSearchRequest request) {
        List<HitSourceDTO> results = elasticsearchDocumentService.hybridSearch(
            request.getKnowledgeBaseId(),
            request.getQuestion(),
            request.getMetadata(),
            request.getIsKnowledge()
        );
        return R.ok(results);
    }

    /**
     * 获取知识库默认配置
     */
    // todo 性能优化
    @SaCheckPermission("system:knowledgeBase:query")
    @GetMapping("/defaultConfig")
    public R<SysKnowledgeBaseConfigVo> getDefaultConfig() {
        SysKnowledgeBaseConfigVo config = new SysKnowledgeBaseConfigVo();
        config.setBlockSize(4000);
        config.setOverlapSize(200);
        config.setTopK(20);
        config.setVectorWeight(0.5f);
        config.setSlicePrompt("{input}\n" +
            "\n" +
            "1、上面的内容是一个文档,根据文档的语义信息将上面的文档划分成不同的片段\n" +
            "2、最终输出一个json数据格式,json里面键是值的标题，json里面的值内容必须是原文中的内容且不要丢失原文信息。\n" +
            "3、json可能存在父子集合\n" +
            "4、将同一个表格数据的内容放到一起,使用markdown格式.\n" +
            "\n" +
            "\n" +
            "输入样例\n" +
            "{\n" +
            "  \"2021年全国教育事业发展统计公报\": {\n" +
            "    \"综合\": \"全国共有各级各类学校[2]52.93万所，各级各类学历教育在校生2.91亿人，专任教师1844.37万人。\",\n" +
            "    \"学前教育\": {\n" +
            "      \"总体情况\": \"全国共有幼儿园29.48万所，比上年增加3117所，增长1.07%。其中，普惠性幼儿园[3]24.47万所，比上年增加1.06万所，增长4.55%，占全国幼儿园的比例83.00%。\",\n" +
            "      \"在园幼儿情况\": \"学前教育在园幼儿[4]4805.21万人，比上年减少13.06万人，下降0.27%。其中，普惠性幼儿园在园幼儿4218.20万人，比上年增加135.37万人，增长3.32%，占全国在园幼儿的比例87.78%，比上年提高3.05个百分点。\",\n" +
            "      \"入园率与师资情况\": \"学前教育毛入园率[5]88.1%，比上年提高2.9个百分点。学前教育专任教师[6]319.10万人，专任教师中专科以上学历比例87.60%。\"\n" +
            "    }\n" +
            "}");
        config.setImagePrompt("提取图片完整的内容，使用中文回答，不要增加额外的文本");

        // 定义需要查询的模块编码列表
        List<String> moduleCodes = Arrays.asList(
            "knowledge_model",
            "image_model",
            "embedding_model",
            "rerank_model"
        );

        // 批量查询所有模块信息（1次查询）
        List<SysModuleVo> modules = sysModuleService.queryByModuleCodes(moduleCodes);

        // 收集所有模块ID
        List<Long> moduleIds = modules.stream()
            .map(SysModuleVo::getModuleId)
            .toList();

        // 批量查询所有模块的默认模型（1次查询）
        Map<Long, Long> defaultModelsMap = sysModuleModelService.queryDefaultModelsByModuleIds(moduleIds);

        // 根据模块编码设置对应的默认模型
        for (SysModuleVo module : modules) {
            Long defaultModelId = defaultModelsMap.get(module.getModuleId());
            if (defaultModelId != null) {
                switch (module.getModuleCode()) {
                    case "knowledge_model":
                        config.setModel(defaultModelId);
                        break;
                    case "image_model":
                        config.setImageModel(defaultModelId);
                        break;
                    case "embedding_model":
                        config.setEmbeddingModel(defaultModelId);
                        break;
                    case "rerank_model":
                        config.setRerankModel(defaultModelId);
                        break;
                }
            }
        }

        return R.ok(config);
    }

}
