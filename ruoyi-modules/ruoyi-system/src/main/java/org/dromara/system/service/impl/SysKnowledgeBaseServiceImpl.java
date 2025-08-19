package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.bo.SysKnowledgeBaseBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.service.IElasticsearchIndexService;
import org.dromara.system.service.ISysKnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 知识库管理Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysKnowledgeBaseServiceImpl implements ISysKnowledgeBaseService {

    private final SysKnowledgeBaseMapper baseMapper;
    private final IElasticsearchIndexService elasticsearchIndexService;

    /**
     * 查询知识库管理
     */
    @Override
    public SysKnowledgeBaseVo queryById(Long knowledgeBaseId) {
        return baseMapper.selectVoById(knowledgeBaseId);
    }

    /**
     * 查询知识库管理列表
     */
    @Override
    public TableDataInfo<SysKnowledgeBaseVo> queryPageList(SysKnowledgeBaseBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysKnowledgeBase> lqw = buildQueryWrapper(bo);
        Page<SysKnowledgeBase> page = pageQuery.build();
        IPage<SysKnowledgeBaseVo> result = baseMapper.selectKnowledgeBaseListVoPage(page, lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库管理列表
     */
    @Override
    public List<SysKnowledgeBaseVo> queryList(SysKnowledgeBaseBo bo) {
        LambdaQueryWrapper<SysKnowledgeBase> lqw = buildQueryWrapper(bo);
        return baseMapper.selectKnowledgeBaseListVo(lqw);
    }

    private LambdaQueryWrapper<SysKnowledgeBase> buildQueryWrapper(SysKnowledgeBaseBo bo) {
        LambdaQueryWrapper<SysKnowledgeBase> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), SysKnowledgeBase::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getDescription()), SysKnowledgeBase::getDescription, bo.getDescription());
        lqw.eq(ObjectUtil.isNotNull(bo.getTopK()), SysKnowledgeBase::getTopK, bo.getTopK());
        lqw.eq(ObjectUtil.isNotNull(bo.getVectorWeight()), SysKnowledgeBase::getVectorWeight, bo.getVectorWeight());
        lqw.like(bo.getModel() != null, SysKnowledgeBase::getModel, bo.getModel());
        lqw.eq(ObjectUtil.isNotNull(bo.getBlockSize()), SysKnowledgeBase::getBlockSize, bo.getBlockSize());
        lqw.eq(ObjectUtil.isNotNull(bo.getOverlapSize()), SysKnowledgeBase::getOverlapSize, bo.getOverlapSize());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysKnowledgeBase::getStatus, bo.getStatus());
        // 注意：del_flag 的过滤已经在 Mapper 的 SQL 中处理，这里不再添加
        return lqw;
    }

    /**
     * 新增知识库管理
     */
    @Override
    public SysKnowledgeBaseVo insertByBo(SysKnowledgeBaseBo bo) {
        SysKnowledgeBase add = MapstructUtils.convert(bo, SysKnowledgeBase.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setKnowledgeBaseId(add.getKnowledgeBaseId());
            try {
                Boolean indexCreated = elasticsearchIndexService.createKnowledgeBaseIndex(add.getKnowledgeBaseId().toString());
                if (!indexCreated) {
                    log.warn("Failed to create Elasticsearch index for knowledge base: {}", add.getKnowledgeBaseId().toString());
                }
            } catch (Exception exception) {
                log.error("Error creating Elasticsearch index for knowledge base: {}", add.getKnowledgeBaseId().toString(), exception);
            }
        }
        return MapstructUtils.convert(add, SysKnowledgeBaseVo.class);
    }

    /**
     * 修改知识库管理
     */
    @Override
    public Boolean updateByBo(SysKnowledgeBaseBo bo) {
        SysKnowledgeBase update = MapstructUtils.convert(bo, SysKnowledgeBase.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysKnowledgeBase entity) {
        // 校验知识库名称唯一性
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<SysKnowledgeBase> lqw = Wrappers.lambdaQuery();
            lqw.eq(SysKnowledgeBase::getName, entity.getName());
            if (ObjectUtil.isNotNull(entity.getKnowledgeBaseId())) {
                lqw.ne(SysKnowledgeBase::getKnowledgeBaseId, entity.getKnowledgeBaseId());
            }
            boolean exists = baseMapper.exists(lqw);
            if (exists) {
                throw new ServiceException("知识库名称已存在");
            }
        }

        // 校验参数值的合理性
        if (ObjectUtil.isNotNull(entity.getTopK()) && entity.getTopK() <= 0) {
            throw new ServiceException("向量检索返回条数必须大于0");
        }
        if (ObjectUtil.isNotNull(entity.getVectorWeight())) {
            if (entity.getVectorWeight() < 0 || entity.getVectorWeight() > 1) {
                throw new ServiceException("向量检索权重必须在0到1之间");
            }
        }
        if (ObjectUtil.isNotNull(entity.getBlockSize()) && entity.getBlockSize() <= 0) {
            throw new ServiceException("块大小必须大于0");
        }
        if (ObjectUtil.isNotNull(entity.getOverlapSize()) && entity.getOverlapSize() < 0) {
            throw new ServiceException("重叠字数不能小于0");
        }
    }

    /**
     * 批量删除知识库管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }

        // Collect knowledge base names before deletion for ES index cleanup
        List<String> knowledgeBaseNames = ids.stream()
            .map(this::queryById)
            .filter(ObjectUtil::isNotNull)
            .map(SysKnowledgeBaseVo::getName)
            .filter(StringUtils::isNotBlank)
            .toList();

        boolean flag = baseMapper.deleteBatchIds(ids) > 0;

        if (flag) {
            // Delete corresponding Elasticsearch indices
            knowledgeBaseNames.forEach(name -> {
                try {
                    Boolean indexDeleted = elasticsearchIndexService.deleteKnowledgeBaseIndex(name);
                    if (!indexDeleted) {
                        log.warn("Failed to delete Elasticsearch index for knowledge base: {}", name);
                    }
                } catch (Exception exception) {
                    log.error("Error deleting Elasticsearch index for knowledge base: {}", name, exception);
                }
            });
        }

        return flag;
    }

    /**
     * 根据知识库名称查询知识库管理
     */
    @Override
    public SysKnowledgeBaseVo queryByName(String name) {
        LambdaQueryWrapper<SysKnowledgeBase> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysKnowledgeBase::getName, name);
        // 过滤已删除的数据
        lqw.eq(SysKnowledgeBase::getDelFlag, SystemConstants.NORMAL);
        lqw.last("LIMIT 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 校验知识库名称是否唯一
     */
    @Override
    public boolean checkNameUnique(SysKnowledgeBaseBo bo) {
        LambdaQueryWrapper<SysKnowledgeBase> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysKnowledgeBase::getName, bo.getName());
        if (ObjectUtil.isNotNull(bo.getKnowledgeBaseId())) {
            lqw.ne(SysKnowledgeBase::getKnowledgeBaseId, bo.getKnowledgeBaseId());
        }
        // 过滤已删除的数据
        lqw.eq(SysKnowledgeBase::getDelFlag, SystemConstants.NORMAL);
        return !baseMapper.exists(lqw);
    }

    /**
     * 修改知识库状态
     */
    @Override
    public int updateKnowledgeBaseStatus(Long knowledgeBaseId, String status) {
        LambdaUpdateWrapper<SysKnowledgeBase> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(SysKnowledgeBase::getKnowledgeBaseId, knowledgeBaseId);
        updateWrapper.set(SysKnowledgeBase::getStatus, status);
        return baseMapper.update(null, updateWrapper);
    }

}
