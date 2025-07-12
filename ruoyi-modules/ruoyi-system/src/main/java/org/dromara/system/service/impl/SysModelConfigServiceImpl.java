package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysModelConfig;
import org.dromara.system.domain.bo.SysModelConfigBo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.mapper.SysModelConfigMapper;
import org.dromara.system.service.ISysModelConfigService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 模型配置Service业务层处理
 *
 * @author 系统管理员
 */
@RequiredArgsConstructor
@Service
public class SysModelConfigServiceImpl implements ISysModelConfigService {

    private final SysModelConfigMapper baseMapper;

    /**
     * 查询模型配置
     */
    @Override
    public SysModelConfigVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询模型配置列表
     */
    @Override
    public TableDataInfo<SysModelConfigVo> queryPageList(SysModelConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysModelConfig> lqw = buildQueryWrapper(bo);
        Page<SysModelConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询模型配置列表
     */
    @Override
    public List<SysModelConfigVo> queryList(SysModelConfigBo bo) {
        LambdaQueryWrapper<SysModelConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysModelConfig> buildQueryWrapper(SysModelConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysModelConfig> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getModelCode()), SysModelConfig::getModelCode, bo.getModelCode());
        lqw.like(StringUtils.isNotBlank(bo.getModelProvider()), SysModelConfig::getModelProvider, bo.getModelProvider());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysModelConfig::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(SysModelConfig::getModelId);
        return lqw;
    }

    /**
     * 新增模型配置
     */
    @Override
    public Boolean insertByBo(SysModelConfigBo bo) {
        SysModelConfig add = MapstructUtils.convert(bo, SysModelConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setModelId(add.getModelId());
        }
        return flag;
    }

    /**
     * 修改模型配置
     */
    @Override
    public Boolean updateByBo(SysModelConfigBo bo) {
        SysModelConfig update = MapstructUtils.convert(bo, SysModelConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysModelConfig entity) {
        // 校验模型编码唯一性
        if (StringUtils.isNotBlank(entity.getModelCode())) {
            boolean exists = baseMapper.exists(new LambdaQueryWrapper<SysModelConfig>()
                .eq(SysModelConfig::getModelCode, entity.getModelCode())
                .ne(ObjectUtil.isNotNull(entity.getModelId()), SysModelConfig::getModelId, entity.getModelId()));
            if (exists) {
                throw new ServiceException("模型编码已存在");
            }
        }
    }

    /**
     * 批量删除模型配置
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据模型编码查询模型配置
     */
    @Override
    public SysModelConfigVo queryByModelCode(String modelCode) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysModelConfig>()
            .eq(SysModelConfig::getModelCode, modelCode));
    }

    /**
     * 根据模型厂商查询模型配置列表
     */
    @Override
    public List<SysModelConfigVo> queryByModelProvider(String modelProvider) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysModelConfig>()
            .eq(SysModelConfig::getModelProvider, modelProvider));
    }

    /**
     * 根据模型类型查询模型配置列表
     */
    @Override
    public List<SysModelConfigVo> queryByModelType(String modelType) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysModelConfig>()
            .apply("JSON_CONTAINS(model_type, {0})", "\"" + modelType + "\""));
    }

    /**
     * 根据模型适配范围查询模型配置列表
     */
    @Override
    public List<SysModelConfigVo> queryByModuleType(String moduleType) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysModelConfig>()
            .apply("JSON_CONTAINS(module_type, {0})", "\"" + moduleType + "\""));
    }

}
