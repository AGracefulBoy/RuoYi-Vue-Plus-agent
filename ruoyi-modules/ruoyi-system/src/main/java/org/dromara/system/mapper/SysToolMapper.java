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
    @Select("SELECT st.tool_id, st.tenant_id, st.tool_name, st.tool_desc, st.function_name, st.tool_type, " +
            "st.is_stream, st.tool_status, st.del_flag, st.create_dept, st.create_by, " +
            "u1.nick_name AS createByName, st.create_time, st.update_by, " +
            "u2.nick_name AS updateByName, st.update_time, st.remark " +
            "FROM sys_tool st " +
            "LEFT JOIN sys_user u1 ON st.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON st.update_by = u2.user_id " +
            "WHERE st.del_flag = '0' ${ew.customSqlSegment}")
    List<SysToolListVo> selectToolListVo(@Param(Constants.WRAPPER) Wrapper<SysTool> wrapper);

    /**
     * 分页查询工具管理列表（不包含脚本代码）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 工具管理列表
     */
    @Select("SELECT st.tool_id, st.tenant_id, st.tool_name, st.tool_desc, st.function_name, st.tool_type, " +
            "st.is_stream, st.tool_status, st.del_flag, st.create_dept, st.create_by, " +
            "u1.nick_name AS createByName, st.create_time, st.update_by, " +
            "u2.nick_name AS updateByName, st.update_time, st.remark " +
            "FROM sys_tool st " +
            "LEFT JOIN sys_user u1 ON st.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON st.update_by = u2.user_id " +
            "WHERE st.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysToolListVo> selectToolListVoPage(IPage<SysTool> page, @Param(Constants.WRAPPER) Wrapper<SysTool> wrapper);

} 