package org.dromara.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jcraft.jsch.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.config.PythonProperties;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.SysToolVenvLog;
import org.dromara.system.domain.bo.PythonDebugRequestBo;
import org.dromara.system.domain.vo.ToolVenvStatusVo;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.mapper.SysToolVenvLogMapper;
import org.dromara.system.service.IToolVirtualEnvService;
import org.dromara.system.util.SshUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工具虚拟环境管理Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ToolVirtualEnvServiceImpl implements IToolVirtualEnvService {

    private final SysToolMapper toolMapper;
    private final SysToolVenvLogMapper venvLogMapper;
    private final PythonProperties pythonProperties;

    private static final String BASE_ENV_PATH = "/python-runtime/env";
    private static final String DEFAULT_PYTHON_VERSION = "3.9";

    /**
     * 为工具创建独立虚拟环境
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createToolVirtualEnv(Long toolId, String pythonVersion) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 验证工具是否存在
            SysTool tool = toolMapper.selectById(toolId);
            if (tool == null) {
                throw new ServiceException("工具不存在: " + toolId);
            }

            // 2. 生成虚拟环境路径
            String venvName = "tool_" + toolId;
            String venvPath = BASE_ENV_PATH + "/" + venvName;

            // 3. 设置Python版本
            if (StringUtils.isBlank(pythonVersion)) {
                pythonVersion = DEFAULT_PYTHON_VERSION;
            }

            // 4. 记录开始创建日志
            logVenvOperation(toolId, "create", "running",
                "开始创建虚拟环境: " + venvPath, null, null, null);

            // 5. 远程创建虚拟环境
            String createCmd = String.format(
                "python%s -m venv %s && echo 'SUCCESS'",
                pythonVersion,
                venvPath
            );

            String result = SshUtil.executeRemoteCommand(
                pythonProperties.getRemote(),
                createCmd
            );

            if (!result.contains("SUCCESS")) {
                throw new ServiceException("虚拟环境创建失败: " + result);
            }

            // 6. 升级pip
            String upgradePipCmd = String.format(
                "%s/bin/pip install --upgrade pip",
                venvPath
            );

            try {
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), upgradePipCmd);
                log.info("pip升级成功: {}", venvPath);
            } catch (Exception e) {
                log.warn("pip升级失败，使用默认版本: {}", e.getMessage());
            }

            // 7. 更新工具记录
            LambdaUpdateWrapper<SysTool> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(SysTool::getToolId, toolId)
                .set(SysTool::getVenvName, venvName)
                .set(SysTool::getVenvPath, venvPath)
                .set(SysTool::getVenvStatus, "ready")
                .set(SysTool::getPythonVersion, pythonVersion)
                .set(SysTool::getVenvCreateTime, new Date())
                .set(SysTool::getVenvLastUpdate, new Date());

            toolMapper.update(null, updateWrapper);

            // 8. 记录成功日志
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            logVenvOperation(toolId, "create", "success",
                "虚拟环境创建成功: " + venvPath, createCmd, result, (int) duration);

            log.info("工具虚拟环境创建成功: toolId={}, venvPath={}", toolId, venvPath);
            return true;

        } catch (Exception e) {
            // 记录失败日志
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            logVenvOperation(toolId, "create", "failed",
                "虚拟环境创建失败: " + e.getMessage(), null, null, (int) duration);

            // 更新工具状态为错误
            LambdaUpdateWrapper<SysTool> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(SysTool::getToolId, toolId)
                .set(SysTool::getVenvStatus, "error");
            toolMapper.update(null, updateWrapper);

            log.error("工具虚拟环境创建失败: toolId={}", toolId, e);
            return false;
        }
    }

    /**
     * 在工具虚拟环境中安装Python包
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String installPackagesInToolEnv(Long toolId, List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "没有需要安装的包";
        }

        // 获取工具信息
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在");
        }

        if (StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境未创建");
        }

        // 构建pip安装命令
        String pipPath = tool.getVenvPath() + "/bin/pip";
        String packagesStr = String.join(" ", packages);
        String installCmd = String.format("%s install %s", pipPath, packagesStr);

        try {
            // 记录开始安装日志
            logVenvOperation(toolId, "install", "running",
                "开始安装包: " + packagesStr, installCmd, null, null);

            // 执行安装命令
            String result = SshUtil.executeRemoteCommand(
                pythonProperties.getRemote(),
                installCmd
            );

            // 更新环境最后更新时间
            LambdaUpdateWrapper<SysTool> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(SysTool::getToolId, toolId)
                .set(SysTool::getVenvLastUpdate, new Date());
            toolMapper.update(null, updateWrapper);

            // 记录成功日志
            logVenvOperation(toolId, "install", "success",
                "包安装成功: " + packagesStr, installCmd, result, null);

            return "包安装成功: " + packagesStr;

        } catch (Exception e) {
            // 记录失败日志
            logVenvOperation(toolId, "install", "failed",
                "包安装失败: " + e.getMessage(), installCmd, null, null);

            throw new ServiceException("包安装失败: " + e.getMessage());
        }
    }

    /**
     * 在工具虚拟环境中卸载Python包
     */
    @Override
    public String uninstallPackagesInToolEnv(Long toolId, List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "没有需要卸载的包";
        }

        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        String pipPath = tool.getVenvPath() + "/bin/pip";
        String packagesStr = String.join(" ", packages);
        String uninstallCmd = String.format("%s uninstall -y %s", pipPath, packagesStr);

        try {
            String result = SshUtil.executeRemoteCommand(
                pythonProperties.getRemote(),
                uninstallCmd
            );

            logVenvOperation(toolId, "uninstall", "success",
                "包卸载成功: " + packagesStr, uninstallCmd, result, null);

            return "包卸载成功: " + packagesStr;
        } catch (Exception e) {
            logVenvOperation(toolId, "uninstall", "failed",
                "包卸载失败: " + e.getMessage(), uninstallCmd, null, null);
            throw new ServiceException("包卸载失败: " + e.getMessage());
        }
    }

    /**
     * 在工具虚拟环境中执行Python代码（流式）
     */
    @Override
    public void executeInToolEnv(Long toolId, String code, String functionName,
                                 Map<String, Object> params, HttpServletResponse response) {
        // 获取工具信息
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        // 检查环境状态
        if (!"ready".equals(tool.getVenvStatus())) {
            throw new ServiceException("虚拟环境状态异常: " + tool.getVenvStatus());
        }

        // 创建临时Python脚本文件
        String scriptFile = String.format("/tmp/tool_%d_%s.py", toolId, System.currentTimeMillis());
        String pythonExec = tool.getVenvPath() + "/bin/python";

        // 构建支持流式输出的Python脚本
        String fullScript = buildStreamingExecutionScript(code, functionName, params);

        log.info("准备流式执行工具 {} 的Python脚本，函数: {}", toolId, functionName);

        try {
            // 设置响应头为SSE格式
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");

            // 1. 写入脚本文件
            String writeCmd = String.format("cat > %s << 'EOF'\n%s\nEOF", scriptFile, fullScript);
            SshUtil.executeRemoteCommand(pythonProperties.getRemote(), writeCmd);

            // 2. 执行脚本并实时获取输出
            String execCmd = String.format("%s -u %s 2>&1", pythonExec, scriptFile);
            log.info("流式执行命令: {}", execCmd);

            // 使用SSH执行并流式读取输出
            boolean success = executeStreamingCommand(pythonProperties.getRemote(), execCmd, response);

            // 3. 清理临时文件
            String cleanCmd = String.format("rm -f %s", scriptFile);
            try {
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), cleanCmd);
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", scriptFile);
            }

            // 如果执行失败，记录日志
            if (!success) {
                log.error("工具 {} 流式执行返回错误状态", toolId);
            }

        } catch (Exception e) {
            log.error("流式执行失败，工具ID: {}", toolId, e);
            try {
                response.getWriter().write("data: " + JSONUtil.toJsonStr(Map.of("error", e.getMessage())) + "\n\n");
                response.getWriter().flush();
            } catch (Exception writeEx) {
                log.error("写入错误信息失败", writeEx);
            }
        }
    }

    /**
     * 在工具虚拟环境中执行Python代码（非流式）
     */
    @Override
    public String executeInToolEnvSync(Long toolId, String code, String functionName,
                                       Map<String, Object> params) {
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        // 创建临时Python脚本
        String scriptFile = String.format("/tmp/tool_%d_%s.py", toolId, System.currentTimeMillis());
        String pythonExec = tool.getVenvPath() + "/bin/python";

        // 构建完整的执行脚本
        String fullScript = buildExecutionScript(code, functionName, params);

        log.info("准备执行工具 {} 的Python脚本，函数: {}, 参数: {}", toolId, functionName, params);
        log.debug("生成的Python脚本: \n{}", fullScript);

        try {
            // 1. 写入脚本文件
            String writeCmd = String.format("cat > %s << 'EOF'\n%s\nEOF", scriptFile, fullScript);
            log.debug("写入脚本文件命令: {}", writeCmd);
            SshUtil.executeRemoteCommand(pythonProperties.getRemote(), writeCmd);

            // 2. 执行脚本，同时捕获错误输出
            String execCmd = String.format("%s %s 2>&1", pythonExec, scriptFile);
            log.info("执行命令: {}", execCmd);

            String result = null;
            try {
                result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), execCmd);
                log.info("执行结果: {}", result);
            } catch (Exception execEx) {
                // 尝试读取脚本内容进行诊断
                log.error("脚本执行失败，尝试读取脚本内容进行诊断");
                try {
                    String catCmd = String.format("cat %s", scriptFile);
                    String scriptContent = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), catCmd);
                    log.error("脚本内容: \n{}", scriptContent);
                } catch (Exception diagEx) {
                    log.error("无法读取脚本内容", diagEx);
                }

                // 检查Python解释器是否存在
                try {
                    String checkPythonCmd = String.format("ls -la %s", pythonExec);
                    String pythonInfo = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), checkPythonCmd);
                    log.info("Python解释器信息: {}", pythonInfo);
                } catch (Exception checkEx) {
                    log.error("Python解释器可能不存在: {}", pythonExec);
                }

                throw execEx;
            }

            // 3. 清理临时文件
            String cleanCmd = String.format("rm -f %s", scriptFile);
            try {
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), cleanCmd);
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", scriptFile);
            }

            return result;

        } catch (Exception e) {
            log.error("代码执行失败，工具ID: {}, 错误: {}", toolId, e.getMessage(), e);

            // 清理临时文件
            try {
                String cleanCmd = String.format("rm -f %s", scriptFile);
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), cleanCmd);
            } catch (Exception cleanEx) {
                log.warn("异常处理中清理临时文件失败: {}", scriptFile);
            }

            throw new ServiceException("代码执行失败: " + e.getMessage());
        }
    }

    /**
     * 删除工具虚拟环境
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteToolVirtualEnv(Long toolId) {
        try {
            SysTool tool = toolMapper.selectById(toolId);
            if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
                return true; // 环境不存在，视为删除成功
            }

            // 删除虚拟环境目录
            String deleteCmd = String.format("rm -rf %s", tool.getVenvPath());
            SshUtil.executeRemoteCommand(pythonProperties.getRemote(), deleteCmd);

            // 更新工具记录
            LambdaUpdateWrapper<SysTool> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(SysTool::getToolId, toolId)
                .set(SysTool::getVenvName, null)
                .set(SysTool::getVenvPath, null)
                .set(SysTool::getVenvStatus, null)
                .set(SysTool::getPythonVersion, null)
                .set(SysTool::getVenvCreateTime, null)
                .set(SysTool::getVenvLastUpdate, null);

            toolMapper.update(null, updateWrapper);

            logVenvOperation(toolId, "delete", "success",
                "虚拟环境删除成功", deleteCmd, null, null);

            return true;

        } catch (Exception e) {
            logVenvOperation(toolId, "delete", "failed",
                "虚拟环境删除失败: " + e.getMessage(), null, null, null);
            return false;
        }
    }

    /**
     * 检查工具虚拟环境状态
     */
    @Override
    public ToolVenvStatusVo checkToolEnvStatus(Long toolId) {
        ToolVenvStatusVo status = new ToolVenvStatusVo();
        status.setToolId(toolId);

        // 获取工具信息
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null) {
            status.setExists(false);
            status.setAvailable(false);
            status.setErrorMessage("工具不存在");
            return status;
        }

        status.setToolName(tool.getToolName());
        status.setVenvName(tool.getVenvName());
        status.setVenvPath(tool.getVenvPath());
        status.setVenvStatus(tool.getVenvStatus());
        status.setPythonVersion(tool.getPythonVersion());
        status.setVenvCreateTime(tool.getVenvCreateTime());
        status.setVenvLastUpdate(tool.getVenvLastUpdate());

        if (StringUtils.isBlank(tool.getVenvPath())) {
            status.setExists(false);
            status.setAvailable(false);
            status.setErrorMessage("虚拟环境未创建");
            return status;
        }

        try {
            // 检查环境是否存在
            String checkCmd = String.format("test -d %s && echo 'EXISTS'", tool.getVenvPath());
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), checkCmd);
            status.setExists(result.contains("EXISTS"));

            if (status.getExists()) {
                // 检查Python是否可执行
                String pythonCheckCmd = String.format("%s/bin/python --version", tool.getVenvPath());
                String pythonVersion = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), pythonCheckCmd);
                status.setAvailable(pythonVersion.contains("Python"));

                // 获取已安装包列表
                try {
                    List<String> packages = getInstalledPackagesInToolEnv(toolId);
                    status.setInstalledPackages(packages);
                    status.setPackageCount(packages.size());
                } catch (Exception e) {
                    log.warn("获取已安装包列表失败: {}", e.getMessage());
                }

                // 获取pip版本
                try {
                    String pipVersionCmd = String.format("%s/bin/pip --version", tool.getVenvPath());
                    String pipVersion = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), pipVersionCmd);
                    status.setPipVersion(pipVersion.split(" ")[1]); // 提取版本号
                } catch (Exception e) {
                    log.warn("获取pip版本失败: {}", e.getMessage());
                }

                // 获取磁盘使用情况
                try {
                    Long diskUsage = getVenvDiskUsage(toolId);
                    status.setDiskUsage(diskUsage);
                    status.setDiskUsageFormatted(formatDiskSize(diskUsage));
                } catch (Exception e) {
                    log.warn("获取磁盘使用情况失败: {}", e.getMessage());
                }

                // 计算健康度评分
                int healthScore = calculateHealthScore(status);
                status.setHealthScore(healthScore);
                status.setHealthDetails(getHealthDetails(healthScore));

            } else {
                status.setAvailable(false);
                status.setErrorMessage("虚拟环境目录不存在");
            }

        } catch (Exception e) {
            status.setExists(false);
            status.setAvailable(false);
            status.setErrorMessage("检查失败: " + e.getMessage());
        }

        return status;
    }

    /**
     * 重建工具虚拟环境
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rebuildToolVirtualEnv(Long toolId) {
        try {
            SysTool tool = toolMapper.selectById(toolId);
            if (tool == null) {
                throw new ServiceException("工具不存在");
            }

            // 1. 获取原环境的包列表
            List<String> installedPackages = new ArrayList<>();
            try {
                installedPackages = getInstalledPackagesInToolEnv(toolId);
            } catch (Exception e) {
                log.warn("获取原环境包列表失败: {}", e.getMessage());
            }

            // 2. 删除原环境
            deleteToolVirtualEnv(toolId);

            // 3. 创建新环境
            String pythonVersion = StringUtils.isNotBlank(tool.getPythonVersion())
                ? tool.getPythonVersion() : DEFAULT_PYTHON_VERSION;
            boolean created = createToolVirtualEnv(toolId, pythonVersion);

            if (created && !installedPackages.isEmpty()) {
                // 4. 重新安装包
                try {
                    installPackagesInToolEnv(toolId, installedPackages);
                } catch (Exception e) {
                    log.warn("重新安装包失败: {}", e.getMessage());
                }
            }

            logVenvOperation(toolId, "rebuild", "success",
                "虚拟环境重建成功", null, null, null);

            return created;

        } catch (Exception e) {
            logVenvOperation(toolId, "rebuild", "failed",
                "虚拟环境重建失败: " + e.getMessage(), null, null, null);
            return false;
        }
    }

    /**
     * 获取工具虚拟环境的已安装包列表
     */
    @Override
    public List<String> getInstalledPackagesInToolEnv(Long toolId) {
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            return new ArrayList<>();
        }

        try {
            String pipPath = tool.getVenvPath() + "/bin/pip";
            String listCmd = String.format("%s list --format=freeze", pipPath);
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), listCmd);

            return Arrays.stream(result.split("\n"))
                .filter(StringUtils::isNotBlank)
                .filter(line -> !line.startsWith("Package") && !line.startsWith("-"))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取已安装包列表失败: toolId={}", toolId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 升级工具虚拟环境中的pip
     */
    @Override
    public String upgradePipInToolEnv(Long toolId) {
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        String pipPath = tool.getVenvPath() + "/bin/pip";
        String upgradeCmd = String.format("%s install --upgrade pip", pipPath);

        try {
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), upgradeCmd);
            return "pip升级成功: " + result;
        } catch (Exception e) {
            throw new ServiceException("pip升级失败: " + e.getMessage());
        }
    }

    /**
     * 生成工具的requirements.txt文件
     */
    @Override
    public String generateRequirements(Long toolId) {
        List<String> packages = getInstalledPackagesInToolEnv(toolId);
        return String.join("\n", packages);
    }

    /**
     * 从requirements.txt安装依赖
     */
    @Override
    public String installFromRequirements(Long toolId, String requirements) {
        if (StringUtils.isBlank(requirements)) {
            return "requirements内容为空";
        }

        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        try {
            // 创建临时requirements文件
            String reqFile = String.format("/tmp/requirements_%d.txt", toolId);
            String writeCmd = String.format("cat > %s << 'EOF'\n%s\nEOF", reqFile, requirements);
            SshUtil.executeRemoteCommand(pythonProperties.getRemote(), writeCmd);

            // 安装依赖
            String pipPath = tool.getVenvPath() + "/bin/pip";
            String installCmd = String.format("%s install -r %s", pipPath, reqFile);
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), installCmd);

            // 清理临时文件
            String cleanCmd = String.format("rm -f %s", reqFile);
            try {
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), cleanCmd);
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", reqFile);
            }

            return "依赖安装成功: " + result;

        } catch (Exception e) {
            throw new ServiceException("依赖安装失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建虚拟环境
     */
    @Override
    public Map<Long, Boolean> batchCreateVirtualEnvs(List<Long> toolIds, String pythonVersion) {
        Map<Long, Boolean> results = new ConcurrentHashMap<>();

        for (Long toolId : toolIds) {
            try {
                boolean success = createToolVirtualEnv(toolId, pythonVersion);
                results.put(toolId, success);
            } catch (Exception e) {
                log.error("批量创建虚拟环境失败: toolId={}", toolId, e);
                results.put(toolId, false);
            }
        }

        return results;
    }

    /**
     * 获取虚拟环境磁盘使用情况
     */
    @Override
    public Long getVenvDiskUsage(Long toolId) {
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            return 0L;
        }

        try {
            String duCmd = String.format("du -sb %s | cut -f1", tool.getVenvPath());
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), duCmd);
            return Long.parseLong(result.trim());
        } catch (Exception e) {
            log.error("获取虚拟环境磁盘使用情况失败: toolId={}", toolId, e);
            return 0L;
        }
    }

    /**
     * 清理虚拟环境缓存
     */
    @Override
    public String cleanVenvCache(Long toolId) {
        SysTool tool = toolMapper.selectById(toolId);
        if (tool == null || StringUtils.isBlank(tool.getVenvPath())) {
            throw new ServiceException("工具虚拟环境不存在");
        }

        try {
            // 清理pip缓存
            String pipPath = tool.getVenvPath() + "/bin/pip";
            String cleanCmd = String.format("%s cache purge", pipPath);
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), cleanCmd);

            // 清理__pycache__目录
            String pycacheCmd = String.format("find %s -type d -name '__pycache__' -exec rm -rf {} +",
                tool.getVenvPath());
            try {
                SshUtil.executeRemoteCommand(pythonProperties.getRemote(), pycacheCmd);
            } catch (Exception e) {
                log.warn("清理__pycache__失败: {}", e.getMessage());
            }

            return "缓存清理成功: " + result;

        } catch (Exception e) {
            throw new ServiceException("缓存清理失败: " + e.getMessage());
        }
    }

    /**
     * 验证Python版本是否可用
     */
    @Override
    public boolean validatePythonVersion(String pythonVersion) {
        try {
            String checkCmd = String.format("python%s --version", pythonVersion);
            String result = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), checkCmd);
            return result.contains("Python " + pythonVersion);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取可用的Python版本列表
     */
    @Override
    public List<String> getAvailablePythonVersions() {
        List<String> versions = Arrays.asList("3.8", "3.9", "3.10", "3.11");
        return versions.stream()
            .filter(this::validatePythonVersion)
            .collect(Collectors.toList());
    }

    /**
     * 记录虚拟环境操作日志
     */
    private void logVenvOperation(Long toolId, String operation, String status,
                                  String message, String command, String result, Integer duration) {
        try {
            SysToolVenvLog log = new SysToolVenvLog();
            log.setToolId(toolId);
            log.setOperation(operation);
            log.setStatus(status);
            log.setMessage(message);
            log.setCommand(command);
            log.setResult(result);
            try {
                log.setOperator(LoginHelper.getUserId());
            } catch (Exception e) {
                log.setOperator(0L);
            }
            log.setCreateTime(new Date());
            log.setDuration(duration);

            venvLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录虚拟环境操作日志失败", e);
        }
    }

    /**
     * 构建流式执行脚本
     */
    private String buildStreamingExecutionScript(String code, String functionName, Map<String, Object> params) {
        StringBuilder script = new StringBuilder();
        script.append("#!/usr/bin/env python\n");
        script.append("# -*- coding: utf-8 -*-\n");
        script.append("import sys\n");
        script.append("import json\n");
        script.append("import traceback\n\n");

        // 添加用户代码
        script.append("# User Code Start\n");
        script.append(code).append("\n");
        script.append("# User Code End\n\n");

        // 主执行逻辑，支持生成器的流式输出
        script.append("if __name__ == '__main__':\n");
        script.append("    try:\n");
        script.append("        # 禁用输出缓冲\n");
        script.append("        sys.stdout.reconfigure(line_buffering=True)\n");
        script.append("        sys.stderr.reconfigure(line_buffering=True)\n");

        if (params != null && !params.isEmpty()) {
            script.append("        params = ").append(JSONUtil.toJsonStr(params)).append("\n");
            script.append("        result = ").append(functionName).append("(**params)\n");
        } else {
            script.append("        result = ").append(functionName).append("()\n");
        }

        // 处理生成器或普通返回值
        script.append("        # 检查是否是生成器\n");
        script.append("        import types\n");
        script.append("        if isinstance(result, types.GeneratorType):\n");
        script.append("            # 流式输出每个yield的值\n");
        script.append("            for chunk in result:\n");
        script.append("                if chunk is not None:\n");
        script.append("                    # 输出SSE格式的数据\n");
        script.append("                    if isinstance(chunk, str):\n");
        script.append("                        print(f'data: {chunk}', flush=True)\n");
        script.append("                    else:\n");
        script.append("                        print(f'data: {json.dumps(chunk, ensure_ascii=False)}', flush=True)\n");
        script.append("                    print('', flush=True)  # 空行分隔\n");
        script.append("            print('data: [DONE]', flush=True)\n");
        script.append("            print('', flush=True)\n");
        script.append("        else:\n");
        script.append("            # 非生成器，直接输出结果\n");
        script.append("            if result is not None:\n");
        script.append("                print(f'data: {json.dumps(result, ensure_ascii=False)}', flush=True)\n");
        script.append("            else:\n");
        script.append("                print('data: {\"success\": true, \"data\": null}', flush=True)\n");
        script.append("            print('', flush=True)\n");
        script.append("            print('data: [DONE]', flush=True)\n");
        script.append("            print('', flush=True)\n");

        script.append("    except Exception as e:\n");
        script.append("        error_info = {\n");
        script.append("            'error': str(e),\n");
        script.append("            'type': type(e).__name__,\n");
        script.append("            'traceback': traceback.format_exc()\n");
        script.append("        }\n");
        script.append("        print(f'data: {json.dumps(error_info, ensure_ascii=False)}', flush=True)\n");
        script.append("        print('', flush=True)\n");
        script.append("        sys.exit(1)\n");

        return script.toString();
    }

    /**
     * 执行流式命令并实时输出
     */
    private boolean executeStreamingCommand(PythonProperties.Remote remoteConfig, String command,
                                        HttpServletResponse response) throws Exception {
        Session session = null;
        Channel channel = null;
        boolean hasError = false;

        try {
            // 创建SSH连接
            JSch jsch = new JSch();

            // 设置私钥
            if (remoteConfig.getPrivateKeyPath() != null && !remoteConfig.getPrivateKeyPath().trim().isEmpty()) {
                if (remoteConfig.getPrivateKeyPassphrase() != null && !remoteConfig.getPrivateKeyPassphrase().trim().isEmpty()) {
                    jsch.addIdentity(remoteConfig.getPrivateKeyPath(), remoteConfig.getPrivateKeyPassphrase());
                } else {
                    jsch.addIdentity(remoteConfig.getPrivateKeyPath());
                }
            }

            // 获取SSH会话
            session = jsch.getSession(remoteConfig.getUsername(), remoteConfig.getHost(), remoteConfig.getPort());

            // 设置密码
            if (remoteConfig.getPassword() != null && !remoteConfig.getPassword().trim().isEmpty()) {
                session.setPassword(remoteConfig.getPassword());
            }

            // 配置SSH会话
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setTimeout(remoteConfig.getConnectionTimeout() * 1000);

            // 连接
            session.connect();

            // 打开执行通道
            channel = session.openChannel("exec");
            ChannelExec execChannel = (ChannelExec) channel;
            execChannel.setCommand(command);

            // 获取输入流和错误流
            InputStream in = execChannel.getInputStream();
            InputStream err = execChannel.getErrStream();
            execChannel.connect();

            // 实时读取并输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));
            PrintWriter writer = response.getWriter();

            // 用于收集所有输出，以便检测错误
            StringBuilder allOutput = new StringBuilder();
            String line;

            // 读取标准输出
            while ((line = reader.readLine()) != null) {
                allOutput.append(line).append("\n");

                // 检测Python错误标志
                if (line.contains("Traceback (most recent call last)") ||
                    line.contains("ModuleNotFoundError") ||
                    line.contains("ImportError") ||
                    line.contains("NameError") ||
                    line.contains("SyntaxError") ||
                    line.contains("AttributeError") ||
                    line.contains("TypeError") ||
                    line.contains("ValueError")) {
                    hasError = true;
                    log.error("检测到Python执行错误: {}", line);
                }

                // 处理SSE格式数据，提取实际内容
                if (line.startsWith("data: ")) {
                    String actualData = line.substring(6); // 移除 "data: " 前缀

                    // 检查是否为结束标志
                    if ("[DONE]".equals(actualData)) {
                        break;
                    }

                    // 只输出实际数据内容，不包含SSE格式
                    if (!actualData.trim().isEmpty()) {
                        writer.write(actualData );
                        writer.flush();
                        log.info("工具流式输出(实际数据): {}", actualData);
                    }
                } else if (line.trim().isEmpty()) {
                    // SSE格式的空行，忽略
                    continue;
                } else {
                    // 非SSE格式的输出（可能是错误信息或其他输出）
                    writer.write(line );
                    writer.flush();
                    log.info("工具流式输出(非SSE): {}", line);
                }
            }

            // 读取错误流（如果有）
            String errorLine;
            StringBuilder errorOutput = new StringBuilder();
            while (errorReader.ready() && (errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
                hasError = true;
            }

            // 如果有错误输出，发送错误信息
            if (errorOutput.length() > 0) {
                log.error("Python脚本错误输出: {}", errorOutput);
                writer.write("data: {\"error\": \"" +
                    errorOutput.toString().replace("\"", "\\\"").replace("\n", "\\n") +
                    "\"}\n\n");
                writer.flush();
            }

            // 等待命令执行完成
            while (!execChannel.isClosed()) {
                Thread.sleep(100);
            }

            // 获取退出状态
            int exitStatus = execChannel.getExitStatus();
            if (exitStatus != 0) {
                hasError = true;
                log.error("Python脚本执行失败，退出状态: {}", exitStatus);

                // 如果没有捕获到具体错误，发送通用错误信息
                if (errorOutput.length() == 0 && !allOutput.toString().contains("data: {\"error\"")) {
                    // 尝试从输出中提取错误信息
                    String errorMsg = extractErrorFromOutput(allOutput.toString());
                    if (errorMsg != null) {
                        writer.write("data: {\"error\": \"" + errorMsg + "\"}\n\n");
                    } else {
                        writer.write("data: {\"error\": \"脚本执行失败，退出状态: " + exitStatus + "\"}\n\n");
                    }
                    writer.flush();
                }
            }

            return !hasError;

        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * 从输出中提取错误信息
     */
    private String extractErrorFromOutput(String output) {
        if (output == null || output.isEmpty()) {
            return null;
        }

        // 查找Python错误的典型模式
        String[] lines = output.split("\n");
        StringBuilder errorMsg = new StringBuilder();
        boolean inTraceback = false;

        for (String line : lines) {
            if (line.contains("Traceback (most recent call last)")) {
                inTraceback = true;
                errorMsg = new StringBuilder();
            }

            if (inTraceback) {
                errorMsg.append(line).append("\\n");

                // 检查是否是错误类型行（通常是traceback的最后一行）
                if (line.matches("^[A-Z][a-zA-Z]*Error:.*") ||
                    line.matches("^[A-Z][a-zA-Z]*Exception:.*")) {
                    return errorMsg.toString();
                }
            }
        }

        // 如果没找到标准的traceback，查找特定错误
        for (String line : lines) {
            if (line.contains("ModuleNotFoundError:")) {
                return "缺少Python模块: " + line.substring(line.indexOf("ModuleNotFoundError:"));
            }
            if (line.contains("ImportError:")) {
                return "导入错误: " + line.substring(line.indexOf("ImportError:"));
            }
            if (line.contains("No module named")) {
                return line;
            }
        }

        return null;
    }

    /**
     * 构建执行脚本
     */
    private String buildExecutionScript(String code, String functionName, Map<String, Object> params) {
        StringBuilder script = new StringBuilder();
        script.append("#!/usr/bin/env python\n");
        script.append("# -*- coding: utf-8 -*-\n");
        script.append("import sys\n");
        script.append("import json\n");
        script.append("import traceback\n\n");

        // 添加用户代码
        script.append("# User Code Start\n");
        script.append(code).append("\n");
        script.append("# User Code End\n\n");

        // 主执行逻辑，包含错误处理
        script.append("if __name__ == '__main__':\n");
        script.append("    try:\n");

        if (params != null && !params.isEmpty()) {
            script.append("        params = ").append(JSONUtil.toJsonStr(params)).append("\n");
            script.append("        result = ").append(functionName).append("(**params)\n");
        } else {
            script.append("        result = ").append(functionName).append("()\n");
        }

        script.append("        if result is not None:\n");
        script.append("            print(json.dumps(result, ensure_ascii=False))\n");
        script.append("        else:\n");
        script.append("            print(json.dumps({'success': True, 'data': None}))\n");

        script.append("    except Exception as e:\n");
        script.append("        error_info = {\n");
        script.append("            'error': str(e),\n");
        script.append("            'type': type(e).__name__,\n");
        script.append("            'traceback': traceback.format_exc()\n");
        script.append("        }\n");
        script.append("        print(json.dumps(error_info, ensure_ascii=False), file=sys.stderr)\n");
        script.append("        sys.exit(1)\n");

        return script.toString();
    }

    /**
     * 计算健康度评分
     */
    private int calculateHealthScore(ToolVenvStatusVo status) {
        int score = 100;

        if (!status.getExists()) {
            return 0;
        }

        if (!status.getAvailable()) {
            return 20;
        }

        // 根据各项指标计算评分
        if (status.getPackageCount() == null || status.getPackageCount() == 0) {
            score -= 10;
        }

        if (StringUtils.isBlank(status.getPipVersion())) {
            score -= 10;
        }

        if (status.getDiskUsage() != null && status.getDiskUsage() > 1024 * 1024 * 1024) { // > 1GB
            score -= 5;
        }

        return Math.max(0, score);
    }

    /**
     * 获取健康详情描述
     */
    private String getHealthDetails(int healthScore) {
        if (healthScore >= 90) {
            return "环境状态良好";
        } else if (healthScore >= 70) {
            return "环境基本正常";
        } else if (healthScore >= 50) {
            return "环境存在问题，建议检查";
        } else if (healthScore > 0) {
            return "环境异常，建议重建";
        } else {
            return "环境不可用";
        }
    }

    /**
     * 格式化磁盘大小
     */
    private String formatDiskSize(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
