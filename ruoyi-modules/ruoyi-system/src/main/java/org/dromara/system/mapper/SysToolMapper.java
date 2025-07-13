package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;

import java.util.List;

/**
 * 工具管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysToolMapper extends BaseMapperPlus<SysTool, SysToolVo> {

    /**
     * 查询工具管理列表（不包含脚本代码）
     *
     * @param wrapper 查询条件
     * @return 工具管理列表
     */
    @Select("SELECT tool_id, tenant_id, tool_name, tool_desc, function_name, tool_type, " +
            "is_stream, tool_status, del_flag, create_dept, create_by, create_time, " +
            "update_by, update_time, remark FROM sys_tool ${ew.customSqlSegment}")
    List<SysToolListVo> selectToolListVo(@Param(Constants.WRAPPER) Wrapper<SysTool> wrapper);

    /**
     * 分页查询工具管理列表（不包含脚本代码）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 工具管理列表
     */
    @Select("SELECT tool_id, tenant_id, tool_name, tool_desc, function_name, tool_type, " +
            "is_stream, tool_status, del_flag, create_dept, create_by, create_time, " +
            "update_by, update_time, remark FROM sys_tool ${ew.customSqlSegment}")
    IPage<SysToolListVo> selectToolListVoPage(IPage<SysTool> page, @Param(Constants.WRAPPER) Wrapper<SysTool> wrapper);

} 