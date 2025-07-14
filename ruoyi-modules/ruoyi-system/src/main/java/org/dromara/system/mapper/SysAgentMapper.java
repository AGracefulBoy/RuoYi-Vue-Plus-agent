package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.vo.SysAgentVo;

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
} 