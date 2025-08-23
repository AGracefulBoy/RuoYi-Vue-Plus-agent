package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.constant.SysKnowledgeBaseDocumentConstants;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentSliceUpdateBo;
import org.dromara.system.domain.vo.KnowledgeBaseMetadata;
import org.dromara.system.domain.vo.SysKnowledgeBaseDocumentVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.service.ISysKnowledgeBaseDocumentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档管理Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysKnowledgeBaseDocumentServiceImpl implements ISysKnowledgeBaseDocumentService {

    private final SysKnowledgeBaseDocumentMapper baseMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 查询知识库文档管理
     */
    @Override
    public SysKnowledgeBaseDocumentVo queryById(Long documentId) {
        SysKnowledgeBaseDocumentVo documentVo = baseMapper.selectVoById(documentId);
        if (ObjectUtil.isNotNull(documentVo) && ObjectUtil.isNotNull(documentVo.getKnowledgeBaseId())) {
            // 设置知识库名称
            var knowledgeBaseVo = knowledgeBaseMapper.selectVoById(documentVo.getKnowledgeBaseId());
            if (ObjectUtil.isNotNull(knowledgeBaseVo)) {
                documentVo.setKnowledgeBaseName(knowledgeBaseVo.getName());
            }
        }
        return documentVo;
    }

    /**
     * 查询知识库文档管理列表
     */
    @Override
    public TableDataInfo<SysKnowledgeBaseDocumentVo> queryPageList(SysKnowledgeBaseDocumentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = buildQueryWrapper(bo);
        Page<SysKnowledgeBaseDocumentVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);

        // 设置知识库名称
        result.getRecords().forEach(this::setKnowledgeBaseName);

        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库文档管理列表
     */
    @Override
    public List<SysKnowledgeBaseDocumentVo> queryList(SysKnowledgeBaseDocumentBo bo) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = buildQueryWrapper(bo);
        List<SysKnowledgeBaseDocumentVo> list = baseMapper.selectVoList(lqw);

        // 设置知识库名称
        list.forEach(this::setKnowledgeBaseName);

        return list;
    }

    /**
     * 根据知识库ID查询文档列表
     */
    @Override
    public List<SysKnowledgeBaseDocumentVo> queryByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysKnowledgeBaseDocument::getKnowledgeBaseId, knowledgeBaseId);
        // 过滤已删除的数据
        lqw.eq(SysKnowledgeBaseDocument::getDelFlag, SystemConstants.NORMAL);
        lqw.orderByDesc(SysKnowledgeBaseDocument::getCreateTime);

        List<SysKnowledgeBaseDocumentVo> list = baseMapper.selectVoList(lqw);

        // 设置知识库名称
        list.forEach(this::setKnowledgeBaseName);

        return list;
    }

    private LambdaQueryWrapper<SysKnowledgeBaseDocument> buildQueryWrapper(SysKnowledgeBaseDocumentBo bo) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getKnowledgeBaseId()), SysKnowledgeBaseDocument::getKnowledgeBaseId, bo.getKnowledgeBaseId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), SysKnowledgeBaseDocument::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), SysKnowledgeBaseDocument::getType, bo.getType());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), SysKnowledgeBaseDocument::getStatus, bo.getStatus());
        lqw.eq(ObjectUtil.isNotNull(bo.getUploadTime()), SysKnowledgeBaseDocument::getUploadTime, bo.getUploadTime());
        // 过滤已删除的数据
        lqw.eq(SysKnowledgeBaseDocument::getDelFlag, SystemConstants.NORMAL);
        lqw.orderByDesc(SysKnowledgeBaseDocument::getCreateTime);
        return lqw;
    }

    /**
     * 设置知识库名称
     */
    private void setKnowledgeBaseName(SysKnowledgeBaseDocumentVo documentVo) {
        if (ObjectUtil.isNotNull(documentVo.getKnowledgeBaseId())) {
            var knowledgeBaseVo = knowledgeBaseMapper.selectVoById(documentVo.getKnowledgeBaseId());
            if (ObjectUtil.isNotNull(knowledgeBaseVo)) {
                documentVo.setKnowledgeBaseName(knowledgeBaseVo.getName());
            }
        }
    }

    /**
     * 新增知识库文档管理
     */
    @Override
    public Boolean insertByBo(SysKnowledgeBaseDocumentBo bo) {
        SysKnowledgeBaseDocument add = MapstructUtils.convert(bo, SysKnowledgeBaseDocument.class);

        // 从知识库管理表获取配置并设置默认值
        fillDefaultConfigFromKnowledgeBase(add);

        validEntityBeforeSave(add);

        // 设置上传时间
        if (ObjectUtil.isNull(add.getUploadTime())) {
            add.setUploadTime(new Date());
        }

        // 设置默认状态为处理中
        if (ObjectUtil.isNull(add.getStatus())) {
            add.setStatus(SysKnowledgeBaseDocumentConstants.DEFAULT_STATUS);
        }

        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setDocumentId(add.getDocumentId());
        }
        return flag;
    }

    /**
     * 批量新增知识库文档管理
     */
    @Override
    public Boolean insertByBoList(List<SysKnowledgeBaseDocumentBo> boList) {
        if (CollUtil.isEmpty(boList)) {
            return true;
        }

        List<SysKnowledgeBaseDocument> entityList = new ArrayList<>();
        Date currentTime = new Date();

        for (SysKnowledgeBaseDocumentBo bo : boList) {
            SysKnowledgeBaseDocument add = MapstructUtils.convert(bo, SysKnowledgeBaseDocument.class);

            // 从知识库管理表获取配置并设置默认值
            fillDefaultConfigFromKnowledgeBase(add);

            validEntityBeforeSave(add);

            // 设置上传时间
            if (ObjectUtil.isNull(add.getUploadTime())) {
                add.setUploadTime(currentTime);
            }

            // 设置默认状态为处理中
            if (ObjectUtil.isNull(add.getStatus())) {
                add.setStatus(SysKnowledgeBaseDocumentConstants.DEFAULT_STATUS);
            }

            entityList.add(add);
        }

        return baseMapper.insertBatch(entityList);
    }

    /**
     * 修改知识库文档管理
     */
    @Override
    public Boolean updateByBo(SysKnowledgeBaseDocumentBo bo) {
        SysKnowledgeBaseDocument update = MapstructUtils.convert(bo, SysKnowledgeBaseDocument.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysKnowledgeBaseDocument entity) {
        // 校验知识库是否存在
        if (ObjectUtil.isNotNull(entity.getKnowledgeBaseId())) {
            var knowledgeBaseVo = knowledgeBaseMapper.selectVoById(entity.getKnowledgeBaseId());
            if (ObjectUtil.isNull(knowledgeBaseVo)) {
                throw new ServiceException("知识库不存在");
            }
        }

        // 校验状态是否有效
        if (StringUtils.isNotBlank(entity.getStatus()) && !SysKnowledgeBaseDocumentConstants.isValidStatus(entity.getStatus())) {
            throw new ServiceException("无效的文档状态: " + entity.getStatus());
        }
    }

    /**
     * 批量删除知识库文档管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 根据知识库ID删除文档
     */
    @Override
    public Boolean deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysKnowledgeBaseDocument::getKnowledgeBaseId, knowledgeBaseId);
        return baseMapper.delete(lqw) >= 0;
    }

    /**
     * 更新文档处理状态
     */
    @Override
    public Boolean updateDocumentStatus(Long documentId, String status) {
        // 校验状态是否有效
        if (!SysKnowledgeBaseDocumentConstants.isValidStatus(status)) {
            throw new ServiceException("无效的文档状态: " + status);
        }

        LambdaUpdateWrapper<SysKnowledgeBaseDocument> luw = Wrappers.lambdaUpdate();
        luw.eq(SysKnowledgeBaseDocument::getDocumentId, documentId);
        luw.set(SysKnowledgeBaseDocument::getStatus, status);
        return baseMapper.update(null, luw) > 0;
    }


    /**
     * 从知识库管理表获取配置并填充到文档中
     */
    private void fillDefaultConfigFromKnowledgeBase(SysKnowledgeBaseDocument document) {
        if (ObjectUtil.isNull(document.getKnowledgeBaseId())) {
            return;
        }

        // 查询知识库配置
        var knowledgeBaseVo = knowledgeBaseMapper.selectVoById(document.getKnowledgeBaseId());
        if (ObjectUtil.isNull(knowledgeBaseVo)) {
            return;
        }

        // 如果文档没有配置 metadata，则从知识库获取
        if (ObjectUtil.isNull(document.getMetadata()) && StringUtils.isNotBlank(knowledgeBaseVo.getMetadata())) {
            document.setMetadata(convertMetadataToMap(knowledgeBaseVo.getMetadata()));
        }

        // 如果文档没有配置 model，则从知识库获取
        if (document.getModel() == null && knowledgeBaseVo.getModel() != null) {
            document.setModel(knowledgeBaseVo.getModel());
        }

        // 如果文档没有配置 embeddingModel，则从知识库获取
        if (document.getEmbeddingModel() == null && knowledgeBaseVo.getEmbeddingModel() != null) {
            document.setEmbeddingModel(knowledgeBaseVo.getEmbeddingModel());
        }

        // 如果文档没有配置 imageModel，则从知识库获取
        if (document.getImageModel() == null && knowledgeBaseVo.getImageModel() != null) {
            document.setImageModel(knowledgeBaseVo.getImageModel());
        }

        // 如果文档没有配置 blockSize，则从知识库获取
        if (ObjectUtil.isNull(document.getBlockSize()) && ObjectUtil.isNotNull(knowledgeBaseVo.getBlockSize())) {
            document.setBlockSize(knowledgeBaseVo.getBlockSize());
        }

        // 如果文档没有配置 overlapSize，则从知识库获取
        if (ObjectUtil.isNull(document.getOverlapSize()) && ObjectUtil.isNotNull(knowledgeBaseVo.getOverlapSize())) {
            document.setOverlapSize(knowledgeBaseVo.getOverlapSize());
        }

        // 如果文档没有配置 slicePrompt，则从知识库获取
        if (StringUtils.isBlank(document.getSlicePrompt()) && StringUtils.isNotBlank(knowledgeBaseVo.getSlicePrompt())) {
            document.setSlicePrompt(knowledgeBaseVo.getSlicePrompt());
        }

        // 如果文档没有配置 imagePrompt，则从知识库获取
        if (StringUtils.isBlank(document.getImagePrompt()) && StringUtils.isNotBlank(knowledgeBaseVo.getImagePrompt())) {
            document.setImagePrompt(knowledgeBaseVo.getImagePrompt());
        }
    }

    /**
     * 将JSON格式的metadata转换为Map对象
     * metadata格式为JSON数组，包含KnowledgeBaseMetadata对象
     */
    private Map<String, Object> convertMetadataToMap(String metadata) {
        Map<String, Object> metadataMap = new HashMap<>();
        if (StringUtils.isBlank(metadata)) {
            return metadataMap;
        }

        try {
            // 解析JSON为List<KnowledgeBaseMetadata>
            List<KnowledgeBaseMetadata> metadataList = JSONUtil.toList(metadata, KnowledgeBaseMetadata.class);

            // 将List转换为Map
            for (KnowledgeBaseMetadata item : metadataList) {
                if (item != null && StringUtils.isNotBlank(item.getName())) {
                    // 使用name作为key，defaultValue作为value
                    String value = StringUtils.isNotBlank(item.getDefaultValue()) ? item.getDefaultValue() : "";
                    metadataMap.put(item.getName(), value);
                }
            }
        } catch (Exception e) {
            log.error("解析metadata JSON失败: {}", metadata, e);
            // 解析失败时返回空Map
        }

        return metadataMap;
    }

    /**
     * 更新文档切片参数
     */
    @Override
    public Boolean updateSliceParams(SysKnowledgeBaseDocumentSliceUpdateBo bo) {
        if (ObjectUtil.isNull(bo.getDocumentId())) {
            throw new ServiceException("文档ID不能为空");
        }

        // 先查询出实体对象
        SysKnowledgeBaseDocument entity = baseMapper.selectById(bo.getDocumentId());
        if (ObjectUtil.isNull(entity)) {
            throw new ServiceException("文档不存在");
        }

        // 更新需要修改的字段
        if (ObjectUtil.isNotNull(bo.getMetadata())) {
            entity.setMetadata(bo.getMetadata());
        }
        if (bo.getModel() != null) {
            entity.setModel(bo.getModel());
        }
        if (ObjectUtil.isNotNull(bo.getBlockSize())) {
            entity.setBlockSize(bo.getBlockSize());
        }
        if (ObjectUtil.isNotNull(bo.getOverlapSize())) {
            entity.setOverlapSize(bo.getOverlapSize());
        }
        if (StringUtils.isNotBlank(bo.getSlicePrompt())) {
            entity.setSlicePrompt(bo.getSlicePrompt());
        }
        if (StringUtils.isNotBlank(bo.getImagePrompt())) {
            entity.setImagePrompt(bo.getImagePrompt());
        }

        // 重要：修改完成后将状态设置为待执行
        entity.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED);

        // 使用updateById更新，这样会正确处理JSON字段
        return baseMapper.updateById(entity) > 0;
    }

}
