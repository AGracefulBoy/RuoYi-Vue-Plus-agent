package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysModelConfig;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.domain.vo.SysUserVo;

/**
 * 模型配置Mapper接口
 *
 * @author 系统管理员
 */
public interface SysModelConfigMapper extends BaseMapperPlus<SysModelConfig, SysModelConfigVo> {

    /**
     * 分页查询用户列表，并进行数据权限控制
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页的用户信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "user_id")
    })
    default Page<SysModelConfigVo> selectPageUserList(Page<SysModelConfig> page, Wrapper<SysModelConfig> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }
}
