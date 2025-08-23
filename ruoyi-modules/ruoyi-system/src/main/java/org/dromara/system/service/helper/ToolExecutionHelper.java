package org.dromara.system.service.helper;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.bo.PythonDebugRequestBo;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.HitDocumentDTO;
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.instruction.ToolCallInstruction;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.service.IElasticsearchDocumentService;
import org.dromara.system.service.ISysToolService;
import org.dromara.system.service.IToolVirtualEnvService;
import org.dromara.system.service.tool.executor.IToolExecutor;
import org.dromara.system.service.tool.executor.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import org.dromara.common.core.exception.ServiceException;
import reactor.core.publisher.FluxSink;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
    private IToolVirtualEnvService virtualEnvService;

    @Autowired
    private List<IToolExecutor> toolExecutors;

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

                // 提取除question之外的其他参数作为metadata
                Map<String, Object> metadataMap = new HashMap<>(params);
                metadataMap.remove("question");
                if (!metadataMap.isEmpty()) {
                    metadata = JSONUtil.toJsonStr(metadataMap);
                }
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
            Long toolId = tool.getToolId();

            // 解析参数
            Map<String, Object> params = null;
            if (StringUtils.hasText(parameters)) {
                params = JSONUtil.toBean(parameters, Map.class);
            }

            // 检查并创建虚拟环境（如果需要）
            if (!StringUtils.hasText(tool.getVenvPath()) || !"ready".equals(tool.getVenvStatus())) {
                log.info("工具 {} 虚拟环境未就绪，开始创建虚拟环境", toolId);
                String pythonVersion = StringUtils.hasText(tool.getPythonVersion()) ?
                    tool.getPythonVersion() : "3.9";

                boolean created = virtualEnvService.createToolVirtualEnv(toolId, pythonVersion);
                if (!created) {
                    throw new ServiceException("虚拟环境创建失败，无法执行脚本");
                }

                // 重新查询工具信息以获取更新后的虚拟环境信息
                tool = toolService.queryById(toolId);
            }

            // 检查是否需要流式输出
            if ("1".equals(tool.getIsStream()) && sink != null && ctx != null) {
                return executeStreamingPythonWithVenv(tool, params, sink, ctx);
            } else {
                return executeNonStreamingPythonWithVenv(tool, params);
            }

        } catch (Exception e) {
            log.error("执行Python脚本失败，工具：{}", tool.getToolName(), e);
            return "Python脚本执行异常: " + e.getMessage();
        }
    }

    /**
     * 使用虚拟环境执行流式Python脚本
     */
    private String executeStreamingPythonWithVenv(SysToolVo tool, Map<String, Object> params,
                                                  FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        log.info("Python脚本流式执行开始（虚拟环境），工具：{}", tool.getToolName());

        // 用于收集所有输出内容
        StringBuilder fullOutput = new StringBuilder();

        // 创建一个虚拟的HttpServletResponse来捕获流式输出
        HttpServletResponse virtualResponse = new FluxSinkResponseWrapper(sink, ctx, fullOutput, streamMessageBuilder);

        try {
            log.info("开始调用虚拟环境服务执行流式脚本，工具ID: {}, 函数: {}, 参数: {}",
                tool.getToolId(), tool.getFunctionName(), params);

            // 调用虚拟环境服务执行脚本
            virtualEnvService.executeInToolEnv(
                tool.getToolId(),
                tool.getScriptCode(),
                tool.getFunctionName(),
                params,
                virtualResponse
            );

            String result = fullOutput.toString();
            log.info("Python脚本流式执行完成，工具：{}，输出长度: {} 字符，完整输出：\n{}",
                tool.getToolName(), result.length(), result);
            return result;

        } catch (Exception e) {
            log.error("Python脚本流式执行失败，工具：{}", tool.getToolName(), e);
            String errorMsg = "执行失败: " + e.getMessage();

            // 如果有部分输出，也返回
            if (!fullOutput.isEmpty()) {
                return fullOutput + "\n" + errorMsg;
            }
            return errorMsg;
        }
    }

    /**
     * 使用虚拟环境执行非流式Python脚本
     */
    private String executeNonStreamingPythonWithVenv(SysToolVo tool, Map<String, Object> params) {
        try {
            // 调用虚拟环境服务同步执行脚本
            String result = virtualEnvService.executeInToolEnvSync(
                tool.getToolId(),
                tool.getScriptCode(),
                tool.getFunctionName(),
                params
            );

            log.info("Python脚本执行完成（虚拟环境），工具：{}，结果：{}", tool.getToolName(), result);
            return result;

        } catch (Exception e) {
            log.error("Python脚本执行失败，工具：{}", tool.getToolName(), e);
            return "执行失败: " + e.getMessage();
        }
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

    /**
     * 虚拟HttpServletResponse实现，用于将流式输出转发到FluxSink
     */
    private static class FluxSinkResponseWrapper implements HttpServletResponse {
        private final FluxSink<StreamMessageResponseDto> sink;
        private final StreamingContext ctx;
        private final StringBuilder fullOutput;
        private final StreamMessageBuilder messageBuilder;
        private final FluxSinkOutputStream outputStream;
        private final PrintWriter printWriter;

        public FluxSinkResponseWrapper(FluxSink<StreamMessageResponseDto> sink,
                                      StreamingContext ctx, StringBuilder fullOutput,
                                      StreamMessageBuilder messageBuilder) {
            this.sink = sink;
            this.ctx = ctx;
            this.fullOutput = fullOutput;
            this.messageBuilder = messageBuilder;
            this.outputStream = new FluxSinkOutputStream();
            this.printWriter = new FluxSinkPrintWriter();
        }

        @Override
        public ServletOutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public PrintWriter getWriter() {
            return printWriter;
        }

        @Override
        public void setContentType(String type) {
            // 忽略内容类型设置
        }

        @Override
        public void setHeader(String name, String value) {
            // 忽略头部设置
        }

        // 以下方法都为空实现，只为了满足接口要求
        @Override public String getCharacterEncoding() { return "UTF-8"; }
        @Override public String getContentType() { return null; }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(Locale loc) {}
        @Override public Locale getLocale() { return Locale.getDefault(); }
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return url; }
        @Override public String encodeRedirectURL(String url) { return url; }
        @Override public void sendError(int sc, String msg) {}
        @Override public void sendError(int sc) {}
        @Override public void sendRedirect(String location) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public void setStatus(int sc) {}
        @Override public int getStatus() { return 200; }
        @Override public String getHeader(String name) { return null; }
        @Override public java.util.Collection<String> getHeaders(String name) { return java.util.Collections.emptyList(); }
        @Override public java.util.Collection<String> getHeaderNames() { return java.util.Collections.emptyList(); }

        private class FluxSinkOutputStream extends ServletOutputStream {
            private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // 不需要实现
            }

            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = lineBuffer.toString(StandardCharsets.UTF_8);

                    // 处理SSE格式的数据
                    if (line.startsWith("data: ")) {
                        line = line.substring(6); // 移除 "data: " 前缀
                    }

                    // 日志记录流式输出内容
                    if (!line.trim().isEmpty()) {
                        log.info("工具流式输出: {}", line);
                    }

                    // 发送流式消息（不添加额外换行符）
                    sink.next(messageBuilder.createStreamMessage(line, "observation", false, ctx));
                    fullOutput.append(line).append('\n'); // 只在fullOutput中保留换行符用于日志
                    lineBuffer.reset();
                } else {
                    lineBuffer.write(b);
                }
            }

            @Override
            public void flush() {
                if (lineBuffer.size() > 0) {
                    String remainingData = lineBuffer.toString(StandardCharsets.UTF_8);
                    if (remainingData.startsWith("data: ")) {
                        remainingData = remainingData.substring(6);
                    }

                    // 日志记录流式输出内容（flush时）
                    if (!remainingData.trim().isEmpty()) {
                        log.info("工具流式输出(flush): {}", remainingData);
                    }

                    // 发送流式消息（flush时，不添加换行符）
                    sink.next(messageBuilder.createStreamMessage(remainingData, "observation", false, ctx));
                    fullOutput.append(remainingData);
                    lineBuffer.reset();
                }
            }
        }

        /**
         * 专门用于FluxSink的PrintWriter实现
         */
        private class FluxSinkPrintWriter extends PrintWriter {
            private final StringBuilder lineBuffer = new StringBuilder();

            public FluxSinkPrintWriter() {
                super(new java.io.StringWriter()); // 虚拟Writer，不会真正使用
            }

            @Override
            public void write(String str) {
                if (str != null) {
                    lineBuffer.append(str);
                }
            }

            @Override
            public void write(char[] buf, int off, int len) {
                if (buf != null && len > 0) {
                    lineBuffer.append(buf, off, len);
                }
            }

            @Override
            public void write(int c) {
                lineBuffer.append((char) c);
            }

            @Override
            public void println(String str) {
                write(str);
                // 注意：这里不自动添加换行符，交由原始内容决定
                flush();
            }

            @Override
            public void flush() {
                if (lineBuffer.length() > 0) {
                    String content = lineBuffer.toString();

                    // 处理SSE格式的数据
                    if (content.startsWith("data: ")) {
                        content = content.substring(6);
                    }

                    // 日志记录流式输出内容
                    if (!content.trim().isEmpty()) {
                        log.info("工具流式输出(PrintWriter): {}", content.trim());
                    }

                    // 发送流式消息（PrintWriter，不添加额外换行符）
                    sink.next(messageBuilder.createStreamMessage(content, "observation", false, ctx));
                    fullOutput.append(content);
                    lineBuffer.setLength(0); // 清空缓冲区
                }
            }

            @Override
            public void close() {
                flush();
            }
        }
    }
}
