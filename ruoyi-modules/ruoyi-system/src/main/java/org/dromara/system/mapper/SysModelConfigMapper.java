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


}
