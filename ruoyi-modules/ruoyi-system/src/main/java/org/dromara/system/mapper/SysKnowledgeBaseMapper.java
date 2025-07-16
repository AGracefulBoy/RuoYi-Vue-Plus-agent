package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;

import java.util.List;

/**
 * 知识库管理Mapper接口
 *
 * @author ruoyi
 */
public interface SysKnowledgeBaseMapper extends BaseMapperPlus<SysKnowledgeBase, SysKnowledgeBaseVo> {

    /**
     * 查询知识库管理列表（包含创建者和更新者名称）
     *
     * @param wrapper 查询条件
     * @return 知识库管理列表
     */
    @Select("SELECT kb.knowledge_base_id, kb.tenant_id, kb.name, kb.description, kb.top_k, " +
            "kb.vector_weight, kb.metadata, kb.model, kb.block_size, kb.overlap_size, " +
            "kb.slice_prompt, kb.image_prompt, kb.status, kb.del_flag, kb.create_dept, " +
            "kb.create_by, u1.nick_name AS createByName, kb.create_time, " +
            "kb.update_by, u2.nick_name AS updateByName, kb.update_time, kb.remark " +
            "FROM sys_knowledge_base kb " +
            "LEFT JOIN sys_user u1 ON kb.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON kb.update_by = u2.user_id " +
            "WHERE kb.del_flag = '0' ${ew.customSqlSegment}")
    List<SysKnowledgeBaseVo> selectKnowledgeBaseListVo(@Param(Constants.WRAPPER) Wrapper<SysKnowledgeBase> wrapper);

    /**
     * 分页查询知识库管理列表（包含创建者和更新者名称）
     *
     * @param page    分页对象
     * @param wrapper 查询条件
     * @return 知识库管理列表
     */
    @Select("SELECT kb.knowledge_base_id, kb.tenant_id, kb.name, kb.description, kb.top_k, " +
            "kb.vector_weight, kb.metadata, kb.model, kb.block_size, kb.overlap_size, " +
            "kb.slice_prompt, kb.image_prompt, kb.status, kb.del_flag, kb.create_dept, " +
            "kb.create_by, u1.nick_name AS createByName, kb.create_time, " +
            "kb.update_by, u2.nick_name AS updateByName, kb.update_time, kb.remark " +
            "FROM sys_knowledge_base kb " +
            "LEFT JOIN sys_user u1 ON kb.create_by = u1.user_id " +
            "LEFT JOIN sys_user u2 ON kb.update_by = u2.user_id " +
            "WHERE kb.del_flag = '0' ${ew.customSqlSegment}")
    IPage<SysKnowledgeBaseVo> selectKnowledgeBaseListVoPage(IPage<SysKnowledgeBase> page, @Param(Constants.WRAPPER) Wrapper<SysKnowledgeBase> wrapper);

} 