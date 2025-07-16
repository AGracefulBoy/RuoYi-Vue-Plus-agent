package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.service.ISysToolService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工具管理Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysToolServiceImpl implements ISysToolService {

    private final SysToolMapper baseMapper;

    /**
     * 查询工具管理（包含脚本代码）
     */
    @Override
    public SysToolVo queryById(Long toolId) {
        return baseMapper.selectVoById(toolId);
    }

    /**
     * 查询工具管理列表（不包含脚本代码）
     */
    @Override
    public TableDataInfo<SysToolListVo> queryPageList(SysToolBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTool> lqw = buildQueryWrapper(bo);
        Page<SysTool> page = pageQuery.build();
        return TableDataInfo.build(baseMapper.selectToolListVoPage(page, lqw));
    }

    /**
     * 查询工具管理列表（不包含脚本代码）
     */
    @Override
    public List<SysToolListVo> queryList(SysToolBo bo) {
        LambdaQueryWrapper<SysTool> lqw = buildQueryWrapper(bo);
        return baseMapper.selectToolListVo(lqw);
    }

    private LambdaQueryWrapper<SysTool> buildQueryWrapper(SysToolBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.like(SysTool::getDelFlag, SystemConstants.NORMAL);
        lqw.like(StringUtils.isNotBlank(bo.getToolName()), SysTool::getToolName, bo.getToolName());
        lqw.like(StringUtils.isNotBlank(bo.getToolDesc()), SysTool::getToolDesc, bo.getToolDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getFunctionName()), SysTool::getFunctionName, bo.getFunctionName());
        lqw.eq(StringUtils.isNotBlank(bo.getToolType()), SysTool::getToolType, bo.getToolType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsStream()), SysTool::getIsStream, bo.getIsStream());
        lqw.eq(StringUtils.isNotBlank(bo.getToolStatus()), SysTool::getToolStatus, bo.getToolStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            SysTool::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        return lqw;
    }

    /**
     * 新增工具管理
     */
    @Override
    public Boolean insertByBo(SysToolBo bo) {
        SysTool add = MapstructUtils.convert(bo, SysTool.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setToolId(add.getToolId());
        }
        return flag;
    }

    /**
     * 修改工具管理
     */
    @Override
    public Boolean updateByBo(SysToolBo bo) {
        SysTool update = MapstructUtils.convert(bo, SysTool.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysTool entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除工具管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验工具名称是否唯一
     */
    @Override
    public boolean checkToolNameUnique(SysToolBo bo) {
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTool::getToolName, bo.getToolName());
        lqw.ne(ObjectUtil.isNotNull(bo.getToolId()), SysTool::getToolId, bo.getToolId());
        return baseMapper.selectCount(lqw) == 0;
    }

    /**
     * 根据工具名称查询工具管理
     */
    @Override
    public SysToolVo queryByToolName(String toolName) {
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTool::getToolName, toolName);
        lqw.last("limit 1");
        return baseMapper.selectVoOne(lqw);
    }

    /**
     * 根据工具类型查询工具管理列表
     */
    @Override
    public List<SysToolVo> queryByToolType(String toolType) {
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTool::getToolType, toolType);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 根据工具状态查询工具管理列表
     */
    @Override
    public List<SysToolVo> queryByToolStatus(String toolStatus) {
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTool::getToolStatus, toolStatus);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 更新工具状态
     */
    @Override
    public Boolean updateToolStatus(Long toolId, String toolStatus) {
        LambdaUpdateWrapper<SysTool> luw = Wrappers.lambdaUpdate();
        luw.set(SysTool::getToolStatus, toolStatus);
        luw.eq(SysTool::getToolId, toolId);
        return baseMapper.update(null, luw) > 0;
    }

}
