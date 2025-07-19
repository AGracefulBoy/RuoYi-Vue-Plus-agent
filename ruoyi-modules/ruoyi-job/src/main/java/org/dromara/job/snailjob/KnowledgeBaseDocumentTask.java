package org.dromara.job.snailjob;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseDocumentTask {

    @Autowired
    private SysKnowledgeBaseDocumentMapper sysKnowledgeBaseDocumentMapper;

    @JobExecutor(name = "excelParsingJob")
    public ExecuteResult excelParsingJob(JobArgs jobArgs) throws InterruptedException {

        SnailJobLog.REMOTE.info("excel 解析: {}", "成功");
        return ExecuteResult.success();
    }

}
