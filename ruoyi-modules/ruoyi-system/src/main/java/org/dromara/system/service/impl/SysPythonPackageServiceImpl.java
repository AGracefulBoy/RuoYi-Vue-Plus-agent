package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysPythonPackage;
import org.dromara.system.domain.bo.SysPythonPackageBo;
import org.dromara.system.domain.vo.SysPythonPackageVo;
import org.dromara.system.mapper.SysPythonPackageMapper;
import org.dromara.system.service.ISysPythonPackageService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Python包管理Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SysPythonPackageServiceImpl implements ISysPythonPackageService {

    private final SysPythonPackageMapper baseMapper;

    /**
     * 查询Python包管理
     */
    @Override
    public SysPythonPackageVo queryById(Long packageId) {
        return baseMapper.selectVoById(packageId);
    }

    /**
     * 查询Python包管理列表
     */
    @Override
    public TableDataInfo<SysPythonPackageVo> queryPageList(SysPythonPackageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysPythonPackage> lqw = buildQueryWrapper(bo);
        Page<SysPythonPackageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询Python包管理列表
     */
    @Override
    public List<SysPythonPackageVo> queryList(SysPythonPackageBo bo) {
        LambdaQueryWrapper<SysPythonPackage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysPythonPackage> buildQueryWrapper(SysPythonPackageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysPythonPackage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getPackageName()), SysPythonPackage::getPackageName, bo.getPackageName());
        lqw.eq(StringUtils.isNotBlank(bo.getPackageVersion()), SysPythonPackage::getPackageVersion, bo.getPackageVersion());
        lqw.like(StringUtils.isNotBlank(bo.getPackageDescription()), SysPythonPackage::getPackageDescription, bo.getPackageDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getIsInstalled()), SysPythonPackage::getIsInstalled, bo.getIsInstalled());
        lqw.like(StringUtils.isNotBlank(bo.getRemark()), SysPythonPackage::getRemark, bo.getRemark());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysPythonPackage::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(SysPythonPackage::getCreateTime);
        return lqw;
    }

    /**
     * 新增Python包管理
     */
    @Override
    public Boolean insertByBo(SysPythonPackageBo bo) {
        SysPythonPackage add = MapstructUtils.convert(bo, SysPythonPackage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setPackageId(add.getPackageId());
        }
        return flag;
    }

    /**
     * 批量新增Python包管理
     */
    @Override
    public Boolean insertBatchByBo(List<SysPythonPackageBo> boList) {
        List<SysPythonPackage> addList = MapstructUtils.convert(boList, SysPythonPackage.class);
        for (SysPythonPackage entity : addList) {
            validEntityBeforeSave(entity);
        }
        return baseMapper.insertBatch(addList);
    }

    /**
     * 修改Python包管理
     */
    @Override
    public Boolean updateByBo(SysPythonPackageBo bo) {
        SysPythonPackage update = MapstructUtils.convert(bo, SysPythonPackage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysPythonPackage entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除Python包管理信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验包名是否唯一
     */
    @Override
    public boolean checkPackageNameUnique(SysPythonPackageBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getPackageName, bo.getPackageName())
            .ne(ObjectUtil.isNotNull(bo.getPackageId()), SysPythonPackage::getPackageId, bo.getPackageId()));
        return !exist;
    }

    /**
     * 根据包名查询Python包管理
     */
    @Override
    public SysPythonPackageVo queryByPackageName(String packageName) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getPackageName, packageName));
    }

    /**
     * 根据安装状态查询Python包管理列表
     */
    @Override
    public List<SysPythonPackageVo> queryByInstallStatus(String isInstalled) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getIsInstalled, isInstalled));
    }

    /**
     * 更新包安装状态
     */
    @Override
    public Boolean updateInstallStatus(Long packageId, String isInstalled) {

        return baseMapper.update(null, new LambdaUpdateWrapper<SysPythonPackage>()
            .set(SysPythonPackage::getIsInstalled, isInstalled)
            .eq(SysPythonPackage::getPackageId, packageId)) > 0;
    }
}
