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
     * 分页查询智能体管理列表（仅返回关键字段）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 智能体管理列表
     */
    @Select("SELECT sa.agent_id, sa.agent_name, sa.agent_desc, sa.agent_type, " +
            "sa.avatar, sa.status, sa.create_by, " +
            "sa.update_by, sa.create_time, sa.update_time " +
            "FROM sys_agent sa " +
            "${ew.customSqlSegment}")
    IPage<SysAgentListVo> selectAgentSimpleListVoPage(IPage<SysAgent> page, @Param(Constants.WRAPPER) Wrapper<SysAgent> wrapper);
}
