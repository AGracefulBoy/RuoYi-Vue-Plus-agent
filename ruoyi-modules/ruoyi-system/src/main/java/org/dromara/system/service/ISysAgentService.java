package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysAgentBo;
import org.dromara.system.domain.vo.SysAgentVo;
import org.dromara.system.domain.vo.SysAgentListVo;

import java.util.Collection;
import java.util.List;

/**
 * 智能体管理Service接口
 *
 * @author 系统管理员
 */
public interface ISysAgentService {

    /**
     * 查询智能体管理
     *
     * @param agentId 智能体ID
     * @return 智能体管理
     */
    SysAgentVo queryById(Long agentId);

    /**
     * 查询智能体管理列表（仅返回关键字段）
     *
     * @param bo        智能体管理业务对象
     * @param pageQuery 分页查询参数
     * @return 智能体管理集合
     */
    TableDataInfo<SysAgentListVo> querySimplePageList(SysAgentBo bo, PageQuery pageQuery);

    /**
     * 查询智能体管理列表
     *
     * @param bo 智能体管理业务对象
     * @return 智能体管理集合
     */
//    List<SysAgentVo> queryList(SysAgentBo bo);

    /**
     * 新增智能体管理
     *
     * @param bo 智能体管理业务对象
     * @return 新增的智能体信息
     */
    SysAgentVo insertByBo(SysAgentBo bo);

    /**
     * 修改智能体管理
     *
     * @param bo 智能体管理业务对象
     * @return 是否修改成功
     */
    Boolean updateByBo(SysAgentBo bo);


    /**
     * 校验并批量删除智能体管理信息
     *
     * @param ids     智能体ID集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 是否删除成功
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验智能体名称是否唯一
     *
     * @param bo 智能体管理业务对象
     * @return 是否唯一
     */
    Boolean checkAgentNameUnique(SysAgentBo bo);
}
