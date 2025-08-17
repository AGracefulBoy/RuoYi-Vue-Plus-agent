package org.dromara.system.service.helper;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.config.PythonProperties;
import org.dromara.system.domain.bo.PythonDebugRequestBo;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.HitDocumentDTO;
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.instruction.ToolCallInstruction;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.service.IElasticsearchDocumentService;
import org.dromara.system.service.ISysToolService;
import org.dromara.system.service.tool.executor.IToolExecutor;
import org.dromara.system.service.tool.executor.ToolExecutionResult;
import org.dromara.system.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.FluxSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行辅助类
 * 负责处理各种工具的执行，包括API工具、Python脚本、知识库查询等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutionHelper {

    @Autowired
    private ISysToolService toolService;

    @Autowired
    private List<IToolExecutor> toolExecutors;

    @Autowired
    private PythonProperties pythonProperties;

    @Autowired
    private IElasticsearchDocumentService elasticsearchDocumentService;

    @Autowired
    private StreamMessageBuilder streamMessageBuilder;

    /**
     * 执行工具调用
     *
     * @param toolCall 工具调用指令
     * @param sink     流式输出sink
     * @param ctx      流式上下文
     * @return 执行结果
     */
    public String executeToolCall(ToolCallInstruction toolCall, FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        try {
            String type = toolCall.getType();
            if ("tool".equals(type)) {
                return executeToolTypeCall(toolCall, sink, ctx);
            } else if ("knowledge".equals(type)) {
                return executeKnowledgeQuery(toolCall);
            } else if ("datasource".equals(type)) {
                // 符合YAGNI原则，暂时返回占位信息
                return "数据源查询功能待实现";
            } else {
                return "不支持的工具类型: " + type;
            }
        } catch (Exception e) {
            log.error("执行工具调用失败", e);
            return "执行异常: " + e.getMessage();
        }
    }

    /**
     * 执行工具类型的调用
     */
    private String executeToolTypeCall(ToolCallInstruction toolCall, FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        // 查询工具配置
        SysToolVo tool = toolService.queryById(toolCall.getId());
        if (tool == null) {
            return "工具不存在: " + toolCall.getToolName();
        }

        // 根据工具类型执行
        if ("2".equals(tool.getToolType())) {
            // Python脚本执行
            return executePythonScript(tool, toolCall.getParameters(), sink, ctx);
        } else {
            // API工具或其他类型
            for (IToolExecutor executor : toolExecutors) {
                if (executor.supports(tool.getToolType())) {
                    ToolExecutionResult result = executor.execute(tool, toolCall.getParameters());
                    return result.isSuccess() ?
                        String.valueOf(result.getData()) :
                        "执行失败: " + result.getErrorMessage();
                }
            }
            return "未找到支持的执行器: " + tool.getToolType();
        }
    }

    /**
     * 执行知识库查询
     */
    private String executeKnowledgeQuery(ToolCallInstruction toolCall) {
        try {
            // 解析参数，获取查询问题
            String parameters = toolCall.getParameters();
            String question = parameters;
            String metadata = null;

            // 如果参数是JSON格式，尝试解析
            if (parameters != null && parameters.trim().startsWith("{")) {
                Map<String, Object> params = JSONUtil.toBean(parameters, Map.class);
                question = (String) params.getOrDefault("question", parameters);
                metadata = params.containsKey("metadata") ? JSONUtil.toJsonStr(params.get("metadata")) : null;
            }

            // 调用ElasticsearchDocumentService进行混合搜索
            List<HitSourceDTO> hitSourceDTOS = elasticsearchDocumentService.hybridSearch(
                toolCall.getId(), question, metadata, true);

            // 格式化搜索结果
            if (hitSourceDTOS == null || hitSourceDTOS.isEmpty()) {
                return "未找到相关文档";
            }

            List<HashMap<String, String>> resultHit = new ArrayList<>();
            for (HitSourceDTO hitSourceDTO : hitSourceDTOS) {
                HitDocumentDTO doc = hitSourceDTO.getHitDocument();
                if (doc != null) {
                    HashMap<String, String> hitSourceMap = new HashMap<>();
                    hitSourceMap.put("content", doc.getContent());
                    hitSourceMap.put("metadata", doc.getMetadata());
                    resultHit.add(hitSourceMap);
                }
            }

            return JSONUtil.toJsonStr(resultHit);
        } catch (Exception e) {
            log.error("执行知识库查询失败", e);
            return "知识库查询失败: " + e.getMessage();
        }
    }

    /**
     * 执行Python脚本
     *
     * @param tool       工具配置
     * @param parameters 参数（JSON格式）
     * @param sink       流式输出sink（可选，用于流式输出）
     * @param ctx        流式上下文（可选，用于流式输出）
     * @return 执行结果
     */
    public String executePythonScript(SysToolVo tool, String parameters, FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        try {
            // 构建Python调试请求
            PythonDebugRequestBo request = new PythonDebugRequestBo();
            request.setCode(tool.getScriptCode());
            request.setFunctionName(tool.getFunctionName());
            request.setStream("1".equals(tool.getIsStream()));

            // 解析参数
            if (StringUtils.hasText(parameters)) {
                Map<String, Object> params = JSONUtil.toBean(parameters, Map.class);
                request.setParams(params);
            }

            // 构建请求数据
            Map<String, String> data = new HashMap<>();
            data.put("code", request.getCode());
            data.put("func_name", request.getFunctionName());
            data.put("params", parameters != null ? parameters : "{}");

            String requestJson = JSONUtil.toJsonStr(data);

            // 检查是否需要流式输出
            if ("1".equals(tool.getIsStream()) && sink != null && ctx != null) {
                return executeStreamingPython(tool, requestJson, sink, ctx);
            } else {
                return executeNonStreamingPython(tool, requestJson);
            }

        } catch (Exception e) {
            log.error("执行Python脚本失败，工具：{}", tool.getToolName(), e);
            return "Python脚本执行异常: " + e.getMessage();
        }
    }

    /**
     * 执行流式Python脚本
     */
    private String executeStreamingPython(SysToolVo tool, String requestJson, FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        log.info("Python脚本流式执行开始，工具：{}", tool.getToolName());

        // 用于收集所有输出内容
        StringBuilder fullOutput = new StringBuilder();

        // 创建自定义输出流，将数据写入FluxSink
        OutputStream streamingOutput = new OutputStream() {
            private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) throws IOException {
                // 当遇到换行符时（字节值为10），发送一行数据
                if (b == '\n') {
                    // 将累积的字节转换为UTF-8字符串（不包含换行符）
                    String line = lineBuffer.toString(StandardCharsets.UTF_8);
                    // 发送流式消息
                    sink.next(streamMessageBuilder.createStreamMessage(line, "observation", false, ctx));
                    // 将内容添加到完整输出中，加上换行符以保持原始格式
                    fullOutput.append(line).append('\n');
                    // 清空缓冲区
                    lineBuffer.reset();
                } else {
                    // 非换行符，添加到缓冲区
                    lineBuffer.write(b);
                }
            }

            @Override
            public void flush() throws IOException {
                // 如果缓冲区还有数据，发送出去
                if (lineBuffer.size() > 0) {
                    // 将累积的字节转换为UTF-8字符串
                    String remainingData = lineBuffer.toString(StandardCharsets.UTF_8);
                    sink.next(streamMessageBuilder.createStreamMessage(remainingData, "observation", false, ctx));
                    // 将内容添加到完整输出中
                    fullOutput.append(remainingData);
                    lineBuffer.reset();
                }
            }
        };

        // 使用流式API
        String streamUrl = pythonProperties.getApi().getStreamUrl();
        boolean success = HttpUtils.sendPostStream(streamUrl, requestJson, streamingOutput);

        if (success) {
            log.info("Python脚本流式执行完成，工具：{}，完整输出：{}", tool.getToolName(), fullOutput.toString());
            // 返回完整的输出内容
            return fullOutput.toString();
        } else {
            log.error("Python脚本流式执行失败，工具：{}", tool.getToolName());
            // 如果有部分输出，也返回
            if (fullOutput.length() > 0) {
                return fullOutput.toString();
            }
            return String.format("Python脚本 %s 执行失败", tool.getToolName());
        }
    }

    /**
     * 执行非流式Python脚本
     */
    private String executeNonStreamingPython(SysToolVo tool, String requestJson) {
        String url = pythonProperties.getApi().getExecUrl();
        String result = HttpUtils.sendPost(url, requestJson);

        log.info("Python脚本执行完成，工具：{}，结果：{}", tool.getToolName(), result);

        // 直接返回结果（Python服务返回的是执行结果的字符串）
        return result;
    }

    /**
     * 检查工具是否配置为流式输出
     *
     * @param toolCall 工具调用指令
     * @return 是否为流式输出工具
     */
    public boolean isStreamingTool(ToolCallInstruction toolCall) {
        try {
            if ("tool".equals(toolCall.getType())) {
                SysToolVo tool = toolService.queryById(toolCall.getId());
                if (tool != null && "2".equals(tool.getToolType())) {
                    // Python脚本类型且配置为流式输出
                    return "1".equals(tool.getIsStream());
                }
            }
        } catch (Exception e) {
            log.error("检查工具流式配置失败", e);
        }
        return false;
    }
}