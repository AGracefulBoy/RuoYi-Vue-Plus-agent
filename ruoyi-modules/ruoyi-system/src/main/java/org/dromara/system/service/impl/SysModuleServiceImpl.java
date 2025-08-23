package org.dromara.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysModelConfig;
import org.dromara.system.domain.SysModule;
import org.dromara.system.domain.SysModuleModel;
import org.dromara.system.domain.bo.SysModuleBo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.domain.vo.SysModuleModelVo;
import org.dromara.system.domain.vo.SysModuleVo;
import org.dromara.system.mapper.SysModelConfigMapper;
import org.dromara.system.mapper.SysModuleMapper;
import org.dromara.system.mapper.SysModuleModelMapper;
import org.dromara.system.service.ISysModuleService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统模块Service业务层处理
 *
 * @author 系统管理员
 */
@RequiredArgsConstructor
@Service
public class SysModuleServiceImpl implements ISysModuleService {

    private final SysModuleMapper baseMapper;
    private final SysModuleModelMapper moduleModelMapper;
    private final SysModelConfigMapper modelConfigMapper;

    /**
     * 查询系统模块
     */
    @Override
    public SysModuleVo queryById(Long moduleId) {
        return baseMapper.selectVoById(moduleId);
    }

    /**
     * 查询系统模块列表
     */
    @Override
    public TableDataInfo<SysModuleVo> queryPageList(SysModuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysModule> lqw = buildQueryWrapper(bo);
        Page<SysModuleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询系统模块列表
     */
    @Override
    public List<SysModuleVo> queryList(SysModuleBo bo) {
        LambdaQueryWrapper<SysModule> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysModule> buildQueryWrapper(SysModuleBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysModule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getModuleCode()), SysModule::getModuleCode, bo.getModuleCode());
        lqw.like(StringUtils.isNotBlank(bo.getModuleName()), SysModule::getModuleName, bo.getModuleName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysModule::getStatus, bo.getStatus());
        lqw.orderByAsc(SysModule::getSortOrder);
        return lqw;
    }

    /**
     * 新增系统模块
     */
    @Override
    public Boolean insertByBo(SysModuleBo bo) {
        SysModule add = MapstructUtils.convert(bo, SysModule.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setModuleId(add.getModuleId());
        }
        return flag;
    }

    /**
     * 修改系统模块
     */
    @Override
    public Boolean updateByBo(SysModuleBo bo) {
        SysModule update = MapstructUtils.convert(bo, SysModule.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysModule entity) {
        // 可以在这里添加业务校验逻辑
    }

    /**
     * 批量删除系统模块
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 根据模块编码查询模块信息
     */
    @Override
    public SysModuleVo queryByModuleCode(String moduleCode) {
        LambdaQueryWrapper<SysModule> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysModule::getModuleCode, moduleCode);
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 根据模块ID查询关联的模型列表
     */
    @Override
    public List<SysModelConfigVo> queryModelsByModuleId(Long moduleId) {
        // 使用 Wrapper 查询模块模型关联关系
        LambdaQueryWrapper<SysModuleModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysModuleModel::getModuleId, moduleId);
        List<SysModuleModelVo> moduleModels = moduleModelMapper.selectVoList(lqw);
        
        // 获取关联的模型ID列表
        List<Long> modelIds = moduleModels.stream()
            .map(SysModuleModelVo::getModelId)
            .distinct()
            .collect(Collectors.toList());
        
        if (modelIds.isEmpty()) {
            return List.of();
        }
        
        // 创建模型ID到isDefault的映射
        Map<Long, Integer> modelDefaultMap = moduleModels.stream()
            .collect(Collectors.toMap(
                SysModuleModelVo::getModelId,
                SysModuleModelVo::getIsDefault,
                (v1, v2) -> v1
            ));
        
        // 批量查询模型配置信息
        List<SysModelConfig> modelConfigs = modelConfigMapper.selectBatchIds(modelIds);
        List<SysModelConfigVo> result = MapstructUtils.convert(modelConfigs, SysModelConfigVo.class);
        
        // 设置isDefault标识
        result.forEach(vo -> vo.setIsDefault(modelDefaultMap.getOrDefault(vo.getModelId(), 0)));
        
        return result;
    }

    /**
     * 根据模块编码查询关联的模型列表
     */
    @Override
    public List<SysModelConfigVo> queryModelsByModuleCode(String moduleCode) {
        SysModuleVo module = queryByModuleCode(moduleCode);
        if (module == null) {
            return List.of();
        }
        return queryModelsByModuleId(module.getModuleId());
    }
    
    /**
     * 批量根据模块编码查询模块信息
     */
    @Override
    public List<SysModuleVo> queryByModuleCodes(List<String> moduleCodes) {
        if (CollUtil.isEmpty(moduleCodes)) {
            return List.of();
        }
        
        LambdaQueryWrapper<SysModule> lqw = Wrappers.lambdaQuery();
        lqw.in(SysModule::getModuleCode, moduleCodes);
        return baseMapper.selectVoList(lqw);
    }
}