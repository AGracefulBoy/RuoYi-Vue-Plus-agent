package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.vo.SysAgentVo;
import org.dromara.system.domain.vo.SysAgentListVo;

import java.util.List;

/**
 * 智能体管理Mapper接口
 *
 * @author 系统管理员
 */
public interface SysAgentMapper extends BaseMapperPlus<SysAgent, SysAgentVo> {

    /**
     * 分页查询智能体管理列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 智能体管理分页列表
     */
    Page<SysAgentVo> selectPageAgentList(@Param("page") Page<SysAgentVo> page, @Param(Constants.WRAPPER) Wrapper<SysAgent> queryWrapper);

    /**
     * 查询智能体管理列表（包含创建者和更新者名称）
     *
     * @param wrapper 查询条件
     * @return 智能体管理列表
     */
    @Select("SELECT sa.agent_id, sa.tenant_id, sa.agent_name, sa.agent_desc, sa.agent_type, " +
            "sa.avatar, sa.prompt_content, sa.common_questions, sa.status, sa.conversation_mode, " +
            "sa.model, sa.enhance_model, sa.tool_list, sa.knowledge_base_list, sa.database_list, " +
            "sa.create_dept, sa.create_by, u1.nick_name AS createByName, sa.create_time, " +
            "sa.update_by, u2.nick_name AS updateByName, sa.update_time, sa.remark " +
            "FROM sys_agent sa " +
            "LEFT JOIN sys_user u1 ON sa.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sa.update_by = u2.user_id " +
            "WHERE sa.del_flag = '0' ${ew.customSqlSegment}")
    List<SysAgentVo> selectAgentListVo(@Param(Constants.WRAPPER) Wrapper<SysAgent> wrapper);

    /**
     * 分页查询智能体管理列表（包含创建者和更新者名称）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 智能体管理列表
     */
    @Select("SELECT sa.agent_id, sa.tenant_id, sa.agent_name, sa.agent_desc, sa.agent_type, " +
            "sa.avatar, sa.prompt_content, sa.common_questions, sa.status, sa.conversation_mode, " +
            "sa.model, sa.enhance_model, sa.tool_list, sa.knowledge_base_list, sa.database_list, " +
            "sa.create_dept, sa.create_by, u1.nick_name AS createByName, sa.create_time, " +
            "sa.update_by, u2.nick_name AS updateByName, sa.update_time, sa.remark " +
            "FROM sys_agent sa " +
            "LEFT JOIN sys_user u1 ON sa.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sa.update_by = u2.user_id " +
            "WHERE sa.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysAgentVo> selectAgentListVoPage(IPage<SysAgent> page, @Param(Constants.WRAPPER) Wrapper<SysAgent> wrapper);

    /**
     * 查询智能体管理列表（仅返回关键字段）
     *
     * @param wrapper 查询条件
     * @return 智能体管理列表
     */
    @Select("SELECT sa.agent_id, sa.agent_name, sa.agent_desc, sa.agent_type, " +
            "sa.avatar, sa.status, sa.create_by, u1.nick_name AS createByName, " +
            "sa.update_by, u2.nick_name AS updateByName, sa.create_time, sa.update_time " +
            "FROM sys_agent sa " +
            "LEFT JOIN sys_user u1 ON sa.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sa.update_by = u2.user_id " +
            "WHERE sa.del_flag = '0' ${ew.customSqlSegment}")
    List<SysAgentListVo> selectAgentSimpleListVo(@Param(Constants.WRAPPER) Wrapper<SysAgent> wrapper);

    /**
     * 分页查询智能体管理列表（仅返回关键字段）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 智能体管理列表
     */
    @Select("SELECT sa.agent_id, sa.agent_name, sa.agent_desc, sa.agent_type, " +
            "sa.avatar, sa.status, sa.create_by, u1.nick_name AS createByName, " +
            "sa.update_by, u2.nick_name AS updateByName, sa.create_time, sa.update_time " +
            "FROM sys_agent sa " +
            "LEFT JOIN sys_user u1 ON sa.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON sa.update_by = u2.user_id " +
            "WHERE sa.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysAgentListVo> selectAgentSimpleListVoPage(IPage<SysAgent> page, @Param(Constants.WRAPPER) Wrapper<SysAgent> wrapper);
} 