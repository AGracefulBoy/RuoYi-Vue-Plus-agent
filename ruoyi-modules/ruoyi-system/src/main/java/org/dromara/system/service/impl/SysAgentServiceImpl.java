package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.bo.SysAgentBo;
import org.dromara.system.domain.vo.SysAgentVo;
import org.dromara.system.mapper.SysAgentMapper;
import org.dromara.system.service.ISysAgentService;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 智能体管理Service业务层处理
 *
 * @author 系统管理员
 */
@RequiredArgsConstructor
@Service
public class SysAgentServiceImpl implements ISysAgentService {

    private final SysAgentMapper baseMapper;

    /**
     * 查询智能体管理
     */
    @Override
    public SysAgentVo queryById(Long agentId) {
        return baseMapper.selectVoById(agentId);
    }

    /**
     * 查询智能体管理列表
     */
    @Override
    public TableDataInfo<SysAgentVo> queryPageList(SysAgentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysAgent> lqw = buildQueryWrapper(bo);
        Page<SysAgentVo> result = baseMapper.selectPageAgentList(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询智能体管理列表
     */
    @Override
    public List<SysAgentVo> queryList(SysAgentBo bo) {
        LambdaQueryWrapper<SysAgent> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysAgent> buildQueryWrapper(SysAgentBo bo) {
        LambdaQueryWrapper<SysAgent> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAgentName()), SysAgent::getAgentName, bo.getAgentName());
        lqw.like(StringUtils.isNotBlank(bo.getAgentDesc()), SysAgent::getAgentDesc, bo.getAgentDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getAgentType()), SysAgent::getAgentType, bo.getAgentType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysAgent::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getConversationMode()), SysAgent::getConversationMode, bo.getConversationMode());
        lqw.like(StringUtils.isNotBlank(bo.getModel()), SysAgent::getModel, bo.getModel());
        lqw.like(StringUtils.isNotBlank(bo.getEnhanceModel()), SysAgent::getEnhanceModel, bo.getEnhanceModel());
        lqw.orderByDesc(SysAgent::getCreateTime);
        return lqw;
    }

    /**
     * 新增智能体管理
     */
    @Override
    public Boolean insertByBo(SysAgentBo bo) {
        SysAgent add = MapstructUtils.convert(bo, SysAgent.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setAgentId(add.getAgentId());
        }
        return flag;
    }

    /**
     * 修改智能体管理
     */
    @Override
    public Boolean updateByBo(SysAgentBo bo) {
        SysAgent update = MapstructUtils.convert(bo, SysAgent.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 修改智能体状态
     */
    @Override
    public Boolean updateAgentStatus(Long agentId, String status) {
        LambdaUpdateWrapper<SysAgent> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysAgent::getAgentId, agentId);
        wrapper.set(SysAgent::getStatus, status);
        return baseMapper.update(null, wrapper) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysAgent entity) {
        // 校验智能体名称唯一性
        if (StringUtils.isNotEmpty(entity.getAgentName())) {
            LambdaQueryWrapper<SysAgent> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(SysAgent::getAgentName, entity.getAgentName());
            if (entity.getAgentId() != null) {
                wrapper.ne(SysAgent::getAgentId, entity.getAgentId());
            }
            long count = baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new ServiceException("智能体名称已存在");
            }
        }

        // 校验智能体类型
        if (StringUtils.isNotEmpty(entity.getAgentType())) {
            String[] validTypes = {"chat", "task", "workflow"};
            boolean isValid = false;
            for (String type : validTypes) {
                if (type.equals(entity.getAgentType())) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new ServiceException("智能体类型不正确，支持的类型：chat、task、workflow");
            }
        }

        // 校验对话模式
        if (StringUtils.isNotEmpty(entity.getConversationMode())) {
            String[] validModes = {"single", "multi", "context"};
            boolean isValid = false;
            for (String mode : validModes) {
                if (mode.equals(entity.getConversationMode())) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new ServiceException("对话模式不正确，支持的模式：single、multi、context");
            }
        }

        // 校验状态
        if (StringUtils.isNotEmpty(entity.getStatus())) {
            if (!"0".equals(entity.getStatus()) && !"1".equals(entity.getStatus())) {
                throw new ServiceException("状态值不正确，0-正常，1-停用");
            }
        }
    }

    /**
     * 批量删除智能体管理
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 删除前校验逻辑（如需要）
            // 例如：校验是否有关联的对话记录等
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验智能体名称是否唯一
     */
    @Override
    public Boolean checkAgentNameUnique(SysAgentBo bo) {
        LambdaQueryWrapper<SysAgent> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAgent::getAgentName, bo.getAgentName());
        if (bo.getAgentId() != null) {
            wrapper.ne(SysAgent::getAgentId, bo.getAgentId());
        }
        long count = baseMapper.selectCount(wrapper);
        return count == 0;
    }

    /**
     * 根据智能体类型查询智能体列表
     */
    @Override
    public List<SysAgentVo> queryByAgentType(String agentType) {
        LambdaQueryWrapper<SysAgent> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAgent::getAgentType, agentType);
        wrapper.eq(SysAgent::getStatus, "0"); // 只查询正常状态的智能体
        wrapper.orderByDesc(SysAgent::getCreateTime);
        return baseMapper.selectVoList(wrapper);
    }

    /**
     * 根据状态查询智能体列表
     */
    @Override
    public List<SysAgentVo> queryByStatus(String status) {
        LambdaQueryWrapper<SysAgent> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAgent::getStatus, status);
        wrapper.orderByDesc(SysAgent::getCreateTime);
        return baseMapper.selectVoList(wrapper);
    }
} 