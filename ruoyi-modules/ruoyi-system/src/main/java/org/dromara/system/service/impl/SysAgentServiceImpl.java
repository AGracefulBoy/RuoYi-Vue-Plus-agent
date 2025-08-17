package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.bo.SysAgentBo;
import org.dromara.system.domain.vo.*;
import org.dromara.system.mapper.SysAgentMapper;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysModelConfigMapper;
import org.dromara.system.service.ISysAgentService;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 智能体管理Service业务层处理
 *
 * @author 系统管理员
 */
@RequiredArgsConstructor
@Service
public class SysAgentServiceImpl implements ISysAgentService {

    private final SysAgentMapper baseMapper;
    private final SysToolMapper toolMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;
    private final SysDatasourceMapper datasourceMapper;
    private final SysModelConfigMapper modelConfigMapper;

    /**
     * 查询智能体管理
     */
    @Override
    public SysAgentVo queryById(Long agentId) {
        SysAgentVo agentVo = baseMapper.selectVoById(agentId);
        if (agentVo == null) {
            return null;
        }

        // 设置模型名称（用于前端展示）
        if (agentVo.getModel() != null) {
            SysModelConfigVo modelConfig = modelConfigMapper.selectVoById(agentVo.getModel());
            if (modelConfig != null) {
                agentVo.setModelName(modelConfig.getModelCode());
            }
        }

        if (agentVo.getEnhanceModel() != null) {
            SysModelConfigVo enhanceModelConfig = modelConfigMapper.selectVoById(agentVo.getEnhanceModel());
            if (enhanceModelConfig != null) {
                agentVo.setEnhanceModelName(enhanceModelConfig.getModelCode());
            }
        }

        // 转换工具列表
        if (CollUtil.isNotEmpty(agentVo.getToolList())) {
            List<SysToolSimpleVo> toolDetailList = new ArrayList<>();
            for (Long toolId : agentVo.getToolList()) {
                SysToolVo toolVo = toolMapper.selectVoById(toolId);
                if (toolVo != null) {
                    SysToolSimpleVo simpleVo = new SysToolSimpleVo();
                    simpleVo.setToolId(toolVo.getToolId());
                    simpleVo.setToolName(toolVo.getToolName());
                    simpleVo.setToolDesc(toolVo.getToolDesc());
                    simpleVo.setFunctionName(toolVo.getFunctionName());
                    simpleVo.setToolType(toolVo.getToolType());
                    simpleVo.setToolStatus(toolVo.getToolStatus());
                    toolDetailList.add(simpleVo);
                }
            }
            agentVo.setToolDetailList(toolDetailList);
        }

        // 转换知识库列表
        if (CollUtil.isNotEmpty(agentVo.getKnowledgeBaseList())) {
            List<SysKnowledgeBaseSimpleVo> knowledgeBaseDetailList = new ArrayList<>();
            for (Long knowledgeBaseId : agentVo.getKnowledgeBaseList()) {
                SysKnowledgeBaseVo knowledgeBaseVo = knowledgeBaseMapper.selectVoById(knowledgeBaseId);
                if (knowledgeBaseVo != null) {
                    SysKnowledgeBaseSimpleVo simpleVo = new SysKnowledgeBaseSimpleVo();
                    simpleVo.setKnowledgeBaseId(knowledgeBaseVo.getKnowledgeBaseId());
                    simpleVo.setName(knowledgeBaseVo.getName());
                    simpleVo.setDescription(knowledgeBaseVo.getDescription());
                    simpleVo.setModel(knowledgeBaseVo.getModel());
                    simpleVo.setStatus(knowledgeBaseVo.getStatus());
                    knowledgeBaseDetailList.add(simpleVo);
                }
            }
            agentVo.setKnowledgeBaseDetailList(knowledgeBaseDetailList);
        }

        // 转换数据库列表
        if (CollUtil.isNotEmpty(agentVo.getDatabaseList())) {
            List<SysDatasourceSimpleVo> databaseDetailList = new ArrayList<>();
            for (Long datasourceId : agentVo.getDatabaseList()) {
                SysDatasourceVo datasourceVo = datasourceMapper.selectVoById(datasourceId);
                if (datasourceVo != null) {
                    SysDatasourceSimpleVo simpleVo = new SysDatasourceSimpleVo();
                    simpleVo.setDatasourceId(datasourceVo.getDatasourceId());
                    simpleVo.setDatasourceName(datasourceVo.getDatasourceName());
                    simpleVo.setDatasourceType(datasourceVo.getDatasourceType());
                    simpleVo.setDatabaseType(datasourceVo.getDatabaseType());
                    simpleVo.setDatabaseName(datasourceVo.getDatabaseName());
                    simpleVo.setStatus(datasourceVo.getStatus());
                    databaseDetailList.add(simpleVo);
                }
            }
            agentVo.setDatabaseDetailList(databaseDetailList);
        }

        return agentVo;
    }

    /**
     * 查询智能体管理列表
     */
//    @Override
//    public TableDataInfo<SysAgentVo> queryPageList(SysAgentBo bo, PageQuery pageQuery) {
//        LambdaQueryWrapper<SysAgent> lqw = buildQueryWrapper(bo);
//        Page<SysAgent> page = pageQuery.build();
//        IPage<SysAgentVo> result = baseMapper.selectAgentListVoPage(page, lqw);
//        return TableDataInfo.build(result);
//    }

    /**
     * 查询智能体管理列表（仅返回关键字段）
     */
    @Override
    public TableDataInfo<SysAgentListVo> querySimplePageList(SysAgentBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysAgent> lqw = buildQueryWrapper(bo);
        Page<SysAgent> page = pageQuery.build();
        IPage<SysAgentListVo> result = baseMapper.selectAgentSimpleListVoPage(page, lqw);
        return TableDataInfo.build(result);
    }

    private LambdaQueryWrapper<SysAgent> buildQueryWrapper(SysAgentBo bo) {
        LambdaQueryWrapper<SysAgent> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getAgentName()), SysAgent::getAgentName, bo.getAgentName());
        lqw.like(StringUtils.isNotBlank(bo.getAgentDesc()), SysAgent::getAgentDesc, bo.getAgentDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getAgentType()), SysAgent::getAgentType, bo.getAgentType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysAgent::getStatus, bo.getStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getConversationMode()), SysAgent::getConversationMode, bo.getConversationMode());
        lqw.eq(ObjectUtil.isNotNull(bo.getModel()), SysAgent::getModel, bo.getModel());
        lqw.eq(ObjectUtil.isNotNull(bo.getEnhanceModel()), SysAgent::getEnhanceModel, bo.getEnhanceModel());
        // 添加 del_flag 过滤条件
        lqw.eq(SysAgent::getDelFlag, "0");
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
        boolean result = baseMapper.updateById(update) > 0;
        
        return result;
    }

    /**
     * 修改智能体状态
     */
    @Override
    public Boolean updateAgentStatus(Long agentId, String status) {
        LambdaUpdateWrapper<SysAgent> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(SysAgent::getAgentId, agentId);
        wrapper.set(SysAgent::getStatus, status);
        boolean result = baseMapper.update(null, wrapper) > 0;
        
        return result;
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
            String[] validModes = {"free_chat", "self_planning"};
            boolean isValid = false;
            for (String mode : validModes) {
                if (mode.equals(entity.getConversationMode())) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) {
                throw new ServiceException("对话模式不正确，支持的模式：free_chat、self_planning");
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
            // 删除前校验逻辑
            // 检查是否有status=0（上架状态）的智能体
            LambdaQueryWrapper<SysAgent> wrapper = Wrappers.lambdaQuery();
            wrapper.in(SysAgent::getAgentId, ids)
                   .eq(SysAgent::getStatus, "0");
            Long count = baseMapper.selectCount(wrapper);
            if (count > 0) {
                throw new ServiceException("不能删除已上架的智能体，请先下架后再删除");
            }
        }
        boolean result = baseMapper.deleteBatchIds(ids) > 0;
        
        return result;
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
        // 过滤已删除的数据
        wrapper.eq(SysAgent::getDelFlag, SystemConstants.NORMAL);
        long count = baseMapper.selectCount(wrapper);
        return count == 0;
    }
}
