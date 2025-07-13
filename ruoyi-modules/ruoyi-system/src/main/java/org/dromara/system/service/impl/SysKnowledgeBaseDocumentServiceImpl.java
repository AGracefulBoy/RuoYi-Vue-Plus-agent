package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseDocumentVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.service.ISysKnowledgeBaseDocumentService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

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
        validEntityBeforeSave(add);
        
        // 设置上传时间
        if (ObjectUtil.isNull(add.getUploadTime())) {
            add.setUploadTime(new Date());
        }
        
        // 设置默认状态为处理中
        if (ObjectUtil.isNull(add.getStatus())) {
            add.setStatus(0);
        }
        
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setDocumentId(add.getDocumentId());
        }
        return flag;
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
        
        // 校验文档名称唯一性
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = Wrappers.lambdaQuery();
            lqw.eq(SysKnowledgeBaseDocument::getName, entity.getName());
            lqw.eq(SysKnowledgeBaseDocument::getKnowledgeBaseId, entity.getKnowledgeBaseId());
            lqw.ne(ObjectUtil.isNotNull(entity.getDocumentId()), SysKnowledgeBaseDocument::getDocumentId, entity.getDocumentId());
            
            boolean exists = baseMapper.exists(lqw);
            if (exists) {
                throw new ServiceException("文档名称已存在");
            }
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
    public Boolean updateDocumentStatus(Long documentId, Integer status) {
        LambdaUpdateWrapper<SysKnowledgeBaseDocument> luw = Wrappers.lambdaUpdate();
        luw.eq(SysKnowledgeBaseDocument::getDocumentId, documentId);
        luw.set(SysKnowledgeBaseDocument::getStatus, status);
        return baseMapper.update(null, luw) > 0;
    }

    /**
     * 校验文档名称是否唯一
     */
    @Override
    public boolean checkNameUnique(SysKnowledgeBaseDocumentBo bo) {
        LambdaQueryWrapper<SysKnowledgeBaseDocument> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysKnowledgeBaseDocument::getName, bo.getName());
        lqw.eq(SysKnowledgeBaseDocument::getKnowledgeBaseId, bo.getKnowledgeBaseId());
        lqw.ne(ObjectUtil.isNotNull(bo.getDocumentId()), SysKnowledgeBaseDocument::getDocumentId, bo.getDocumentId());
        return !baseMapper.exists(lqw);
    }

} 