package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.bo.PythonDebugRequestBo;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.bo.ToolDebugRequestBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.service.ISysPythonPackageService;
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
    private final ISysPythonPackageService pythonPackageService;

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
        // 注意：del_flag 的过滤已经在 Mapper 的 SQL 中处理，这里不再添加
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

    /**
     * 复制工具管理
     */
    @Override
    public Boolean copyTool(Long toolId) {
        // 查询原工具信息
        SysToolVo originalTool = baseMapper.selectVoById(toolId);
        if (ObjectUtil.isNull(originalTool)) {
            return false;
        }

        // 手动创建新的业务对象并复制属性
        SysToolBo copyToolBo = new SysToolBo();
        
        // 复制基本属性
        copyToolBo.setToolName(originalTool.getToolName());
        copyToolBo.setToolDesc(originalTool.getToolDesc());
        copyToolBo.setFunctionName(originalTool.getFunctionName());
        copyToolBo.setToolType(originalTool.getToolType());
        copyToolBo.setIsStream(originalTool.getIsStream());
        copyToolBo.setScriptCode(originalTool.getScriptCode());
        copyToolBo.setToolStatus(originalTool.getToolStatus());
        copyToolBo.setRemark(originalTool.getRemark());

        // 生成新的工具名称：原工具名 + 副本 + 时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newToolName = originalTool.getToolName() + "副本" + timestamp;
        copyToolBo.setToolName(newToolName);

        // 清空ID，让系统自动生成新的ID
        copyToolBo.setToolId(null);

        // 设置创建时间和更新时间为空，让系统自动填充
        copyToolBo.setCreateTime(null);
        copyToolBo.setUpdateTime(null);

        // 插入新的工具记录
        return insertByBo(copyToolBo);
    }

    /**
     * 根据工具ID执行Python代码调试
     */
    @Override
    public void debugToolCode(ToolDebugRequestBo request, HttpServletResponse response) {
        // 1. 根据工具ID查询工具信息
        SysToolVo tool = queryById(request.getToolId());
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }

        // 2. 验证工具状态
        if (!"0".equals(tool.getToolStatus())) {
            throw new ServiceException("工具已停用");
        }

        // 3. 验证工具类型
        if (!"script".equals(tool.getToolType())) {
            throw new ServiceException("该工具不是脚本类型，无法调试");
        }

        // 4. 验证脚本代码是否存在
        if (StringUtils.isBlank(tool.getScriptCode())) {
            throw new ServiceException("工具脚本代码为空");
        }

        // 5. 构建Python调试请求对象
        PythonDebugRequestBo pythonRequest = new PythonDebugRequestBo();
        pythonRequest.setCode(tool.getScriptCode());
        pythonRequest.setFuncName(tool.getFunctionName());
        pythonRequest.setParams(request.getParams());
        pythonRequest.setStream(request.getStream());

        // 6. 记录调试信息
        log.info("开始调试工具：{}, 工具ID：{}, 函数名：{}, 流式：{}", 
            tool.getToolName(), tool.getToolId(), tool.getFunctionName(), request.getStream());

        // 7. 调用Python调试服务
        pythonPackageService.debugPythonCode(pythonRequest, response);
    }

}
