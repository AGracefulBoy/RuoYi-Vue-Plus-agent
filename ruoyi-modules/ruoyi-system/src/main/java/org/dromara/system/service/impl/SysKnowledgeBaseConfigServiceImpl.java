package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysKnowledgeBaseConfig;
import org.dromara.system.domain.bo.SysKnowledgeBaseConfigBo;
import org.dromara.system.domain.vo.SysKnowledgeBaseConfigVo;
import org.dromara.system.mapper.SysKnowledgeBaseConfigMapper;
import org.dromara.system.service.ISysKnowledgeBaseConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 知识库默认配置Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysKnowledgeBaseConfigServiceImpl implements ISysKnowledgeBaseConfigService {

    private final SysKnowledgeBaseConfigMapper baseMapper;

    /**
     * 查询知识库默认配置
     *
     * @param knowledgeBaseConfigId 知识库默认配置主键
     * @return 知识库默认配置
     */
    @Override
    public SysKnowledgeBaseConfigVo queryById(Long knowledgeBaseConfigId) {
        return baseMapper.selectVoById(knowledgeBaseConfigId);
    }

    /**
     * 查询知识库默认配置列表
     *
     * @param bo        知识库默认配置
     * @param pageQuery 分页参数
     * @return 知识库默认配置
     */
    @Override
    public TableDataInfo<SysKnowledgeBaseConfigVo> queryPageList(SysKnowledgeBaseConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysKnowledgeBaseConfig> lqw = buildQueryWrapper(bo);
        Page<SysKnowledgeBaseConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库默认配置列表
     *
     * @param bo 知识库默认配置
     * @return 知识库默认配置
     */
    @Override
    public List<SysKnowledgeBaseConfigVo> queryList(SysKnowledgeBaseConfigBo bo) {
        LambdaQueryWrapper<SysKnowledgeBaseConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysKnowledgeBaseConfig> buildQueryWrapper(SysKnowledgeBaseConfigBo bo) {
        LambdaQueryWrapper<SysKnowledgeBaseConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(ObjectUtil.isNotNull(bo.getTopK()), SysKnowledgeBaseConfig::getTopK, bo.getTopK());
        lqw.eq(ObjectUtil.isNotNull(bo.getVectorWeight()), SysKnowledgeBaseConfig::getVectorWeight, bo.getVectorWeight());
        lqw.like(StringUtils.isNotBlank(bo.getMetadata()), SysKnowledgeBaseConfig::getMetadata, bo.getMetadata());
        lqw.like(StringUtils.isNotBlank(bo.getModel()), SysKnowledgeBaseConfig::getModel, bo.getModel());
        lqw.eq(ObjectUtil.isNotNull(bo.getBlockSize()), SysKnowledgeBaseConfig::getBlockSize, bo.getBlockSize());
        lqw.eq(ObjectUtil.isNotNull(bo.getOverlapSize()), SysKnowledgeBaseConfig::getOverlapSize, bo.getOverlapSize());
        lqw.like(StringUtils.isNotBlank(bo.getSlicePrompt()), SysKnowledgeBaseConfig::getSlicePrompt, bo.getSlicePrompt());
        lqw.like(StringUtils.isNotBlank(bo.getImagePrompt()), SysKnowledgeBaseConfig::getImagePrompt, bo.getImagePrompt());
        lqw.like(StringUtils.isNotBlank(bo.getRemark()), SysKnowledgeBaseConfig::getRemark, bo.getRemark());
        return lqw;
    }

    /**
     * 新增知识库默认配置
     *
     * @param bo 知识库默认配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysKnowledgeBaseConfigBo bo) {
        SysKnowledgeBaseConfig add = MapstructUtils.convert(bo, SysKnowledgeBaseConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setKnowledgeBaseConfigId(add.getKnowledgeBaseConfigId());
        }
        return flag;
    }

    /**
     * 修改知识库默认配置
     *
     * @param bo 知识库默认配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysKnowledgeBaseConfigBo bo) {
        SysKnowledgeBaseConfig update = MapstructUtils.convert(bo, SysKnowledgeBaseConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(SysKnowledgeBaseConfig entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库默认配置
     *
     * @param ids     需要删除的知识库默认配置主键
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 获取默认配置
     *
     * @return 默认配置
     */
    @Override
    public SysKnowledgeBaseConfigVo getDefaultConfig() {
        LambdaQueryWrapper<SysKnowledgeBaseConfig> lqw = Wrappers.lambdaQuery();
        lqw.orderByDesc(SysKnowledgeBaseConfig::getCreateTime);
        lqw.last("LIMIT 1");
        SysKnowledgeBaseConfigVo config = baseMapper.selectVoOne(lqw);
        
        // 如果没有配置，返回默认值
        if (config == null) {
            config = new SysKnowledgeBaseConfigVo();
            config.setTopK(5);
            config.setVectorWeight(0.7f);
            config.setBlockSize(512);
            config.setOverlapSize(50);
            config.setModel("text-embedding-ada-002");
            config.setSlicePrompt("请将以下文档切分为合适的片段：");
            config.setImagePrompt("请描述图片内容：");
        }
        
        return config;
    }

} 