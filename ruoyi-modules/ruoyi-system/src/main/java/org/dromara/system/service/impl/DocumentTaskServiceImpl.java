package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.domain.DocumentTask;
import org.dromara.system.mapper.DocumentTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档任务Service业务层处理
 */
@RequiredArgsConstructor
@Service
public class DocumentTaskServiceImpl {

    private final DocumentTaskMapper documentTaskMapper;

    /**
     * 根据任务ID和状态查询文档任务列表
     *
     * @param taskId 任务ID
     * @param status 状态
     * @return 文档任务列表
     */
    public List<DocumentTask> queryByTaskIdAndStatus(String taskId, String status) {
        LambdaQueryWrapper<DocumentTask> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StringUtils.isNotBlank(taskId), DocumentTask::getTaskId, taskId);
        queryWrapper.eq(StringUtils.isNotBlank(status), DocumentTask::getStatus, status);
        queryWrapper.orderByDesc(DocumentTask::getCreatedTime);
        
        return documentTaskMapper.selectList(queryWrapper);
    }
    
    /**
     * 根据任务ID列表和状态批量查询文档任务
     *
     * @param taskIds 任务ID列表
     * @param status 状态
     * @return 文档任务列表
     */
    public List<DocumentTask> queryByTaskIdsAndStatus(List<String> taskIds, String status) {
        if (taskIds == null || taskIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<DocumentTask> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.in(DocumentTask::getTaskId, taskIds);
        queryWrapper.eq(StringUtils.isNotBlank(status), DocumentTask::getStatus, status);
        queryWrapper.orderByDesc(DocumentTask::getCreatedTime);
        
        return documentTaskMapper.selectList(queryWrapper);
    }
}