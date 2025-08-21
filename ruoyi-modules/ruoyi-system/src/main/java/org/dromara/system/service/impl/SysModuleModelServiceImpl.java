package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysModuleModel;
import org.dromara.system.domain.bo.SysModuleModelBo;
import org.dromara.system.domain.vo.SysModuleModelVo;
import org.dromara.system.mapper.SysModuleModelMapper;
import org.dromara.system.service.ISysModuleModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块模型关联Service业务层处理
 *
 * @author 系统管理员
 */
@RequiredArgsConstructor
@Service
public class SysModuleModelServiceImpl implements ISysModuleModelService {

    private final SysModuleModelMapper baseMapper;

    /**
     * 查询模块模型关联
     */
    @Override
    public SysModuleModelVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询模块模型关联列表
     */
    @Override
    public TableDataInfo<SysModuleModelVo> queryPageList(SysModuleModelBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysModuleModel> lqw = buildQueryWrapper(bo);
        Page<SysModuleModelVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询模块模型关联列表
     */
    @Override
    public List<SysModuleModelVo> queryList(SysModuleModelBo bo) {
        LambdaQueryWrapper<SysModuleModel> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysModuleModel> buildQueryWrapper(SysModuleModelBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysModuleModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getModuleId() != null, SysModuleModel::getModuleId, bo.getModuleId());
        lqw.eq(bo.getModelId() != null, SysModuleModel::getModelId, bo.getModelId());
        return lqw;
    }

    /**
     * 新增模块模型关联
     */
    @Override
    public Boolean insertByBo(SysModuleModelBo bo) {
        SysModuleModel add = MapstructUtils.convert(bo, SysModuleModel.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改模块模型关联
     */
    @Override
    public Boolean updateByBo(SysModuleModelBo bo) {
        SysModuleModel update = MapstructUtils.convert(bo, SysModuleModel.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysModuleModel entity) {
        // 可以在这里添加业务校验逻辑
    }

    /**
     * 批量删除模块模型关联
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 绑定模块与模型关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean bindModuleModels(Long moduleId, List<Long> modelIds) {
        if (CollUtil.isEmpty(modelIds)) {
            return true;
        }

        // 查询已存在的关联关系
        LambdaQueryWrapper<SysModuleModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysModuleModel::getModuleId, moduleId);
        lqw.in(SysModuleModel::getModelId, modelIds);
        List<SysModuleModel> existList = baseMapper.selectList(lqw);

        // 获取已存在的模型ID
        List<Long> existModelIds = existList.stream()
            .map(SysModuleModel::getModelId)
            .collect(Collectors.toList());

        // 过滤出需要新增的模型ID
        List<Long> needAddModelIds = modelIds.stream()
            .filter(modelId -> !existModelIds.contains(modelId))
            .collect(Collectors.toList());

        // 批量插入新的关联关系
        if (CollUtil.isNotEmpty(needAddModelIds)) {
            List<SysModuleModel> addList = new ArrayList<>();
            for (Long modelId : needAddModelIds) {
                SysModuleModel model = new SysModuleModel();
                model.setModuleId(moduleId);
                model.setModelId(modelId);
                addList.add(model);
            }
            return baseMapper.insertBatch(addList);
        }

        return true;
    }

    /**
     * 解绑模块与模型关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbindModuleModels(Long moduleId, List<Long> modelIds) {
        if (CollUtil.isEmpty(modelIds)) {
            return true;
        }

        LambdaQueryWrapper<SysModuleModel> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysModuleModel::getModuleId, moduleId);
        lqw.in(SysModuleModel::getModelId, modelIds);
        return baseMapper.delete(lqw) > 0;
    }

    /**
     * 设置模块的默认模型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultModel(Long moduleId, Long modelId) {
        // 1. 先检查该模型是否已绑定到模块
        LambdaQueryWrapper<SysModuleModel> checkLqw = Wrappers.lambdaQuery();
        checkLqw.eq(SysModuleModel::getModuleId, moduleId);
        checkLqw.eq(SysModuleModel::getModelId, modelId);
        SysModuleModel moduleModel = baseMapper.selectOne(checkLqw);
        
        if (moduleModel == null) {
            // 如果模型未绑定到模块，先进行绑定
            moduleModel = new SysModuleModel();
            moduleModel.setModuleId(moduleId);
            moduleModel.setModelId(modelId);
            moduleModel.setIsDefault(1);
            return baseMapper.insert(moduleModel) > 0;
        }
        
        // 2. 将该模块下所有模型的 isDefault 设置为 0
        SysModuleModel resetDefault = new SysModuleModel();
        resetDefault.setIsDefault(0);
        LambdaQueryWrapper<SysModuleModel> resetLqw = Wrappers.lambdaQuery();
        resetLqw.eq(SysModuleModel::getModuleId, moduleId);
        baseMapper.update(resetDefault, resetLqw);
        
        // 3. 将指定模型的 isDefault 设置为 1
        SysModuleModel setDefault = new SysModuleModel();
        setDefault.setIsDefault(1);
        LambdaQueryWrapper<SysModuleModel> setLqw = Wrappers.lambdaQuery();
        setLqw.eq(SysModuleModel::getModuleId, moduleId);
        setLqw.eq(SysModuleModel::getModelId, modelId);
        
        return baseMapper.update(setDefault, setLqw) > 0;
    }
}
