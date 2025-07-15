package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.base.BaseException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.config.PythonProperties;
import org.dromara.system.domain.SysPythonPackage;
import org.dromara.system.domain.bo.SysPythonPackageBo;
import org.dromara.system.domain.vo.SysPythonPackageVo;
import org.dromara.system.mapper.SysPythonPackageMapper;
import org.dromara.system.service.ISysPythonPackageService;
import org.dromara.system.util.SshUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Python包管理Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysPythonPackageServiceImpl implements ISysPythonPackageService {

    private final SysPythonPackageMapper baseMapper;

    private final PythonProperties pythonProperties;

    private static final String[] allowedChars = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
        "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "_", "-", "/", "@", ".", "="};

    /**
     * 查询Python包管理
     */
    @Override
    public SysPythonPackageVo queryById(Long packageId) {
        return baseMapper.selectVoById(packageId);
    }

    /**
     * 查询Python包管理列表
     */
    @Override
    public TableDataInfo<SysPythonPackageVo> queryPageList(SysPythonPackageBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysPythonPackage> lqw = buildQueryWrapper(bo);
        Page<SysPythonPackageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询Python包管理列表
     */
    @Override
    public List<SysPythonPackageVo> queryList(SysPythonPackageBo bo) {
        LambdaQueryWrapper<SysPythonPackage> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysPythonPackage> buildQueryWrapper(SysPythonPackageBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysPythonPackage> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getPackageName()), SysPythonPackage::getPackageName, bo.getPackageName());
        lqw.eq(StringUtils.isNotBlank(bo.getPackageVersion()), SysPythonPackage::getPackageVersion, bo.getPackageVersion());
        lqw.like(StringUtils.isNotBlank(bo.getPackageDescription()), SysPythonPackage::getPackageDescription, bo.getPackageDescription());
        lqw.between(params.get("beginTime") != null && params.get("endTime") != null,
            SysPythonPackage::getCreateTime, params.get("beginTime"), params.get("endTime"));
        lqw.orderByDesc(SysPythonPackage::getCreateTime);
        return lqw;
    }

    /**
     * 新增Python包管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysPythonPackageBo bo) {
        SysPythonPackage add = MapstructUtils.convert(bo, SysPythonPackage.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setPackageId(add.getPackageId());

            // 在创建Python包时自动执行依赖安装逻辑
            try {
                // 构建包名字符串，包含版本信息
                String packageStr = bo.getPackageName();
                if (StringUtils.isNotBlank(bo.getPackageVersion())) {
                    packageStr += "==" + bo.getPackageVersion();
                }

                // 执行安装
                String cleanedPackages = cleanPackages(packageStr);
                execPip("install", cleanedPackages);

                // 更新安装状态为已安装
                updateInstallStatus(add.getPackageId(), "1");

                log.info("创建Python包成功并自动安装依赖：{}", cleanedPackages);
            } catch (Exception e) {
                log.error("创建Python包后自动安装依赖失败：{}", e.getMessage(), e);
                // 这里可以选择是否抛出异常，根据业务需求决定
                // 如果希望即使安装失败也能创建包记录，就不抛出异常
            }
        }
        return flag;
    }

    /**
     * 批量新增Python包管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertBatchByBo(List<SysPythonPackageBo> boList) {
        List<SysPythonPackage> addList = MapstructUtils.convert(boList, SysPythonPackage.class);
        for (SysPythonPackage entity : addList) {
            validEntityBeforeSave(entity);
        }
        boolean result = baseMapper.insertBatch(addList);

        // 在创建Python包时自动执行依赖安装逻辑
        if (result) {
            try {
                // 构建包名字符串，包含版本信息
                StringBuilder packagesStr = new StringBuilder();
                for (int i = 0; i < boList.size(); i++) {
                    SysPythonPackageBo bo = boList.get(i);
                    if (i > 0) {
                        packagesStr.append(" ");
                    }
                    packagesStr.append(bo.getPackageName());
                    if (StringUtils.isNotBlank(bo.getPackageVersion())) {
                        packagesStr.append("==").append(bo.getPackageVersion());
                    }
                }

                // 执行安装
                String cleanedPackages = cleanPackages(packagesStr.toString());
                execPip("install", cleanedPackages);

                // 更新安装状态为已安装
                for (SysPythonPackage entity : addList) {
                    updateInstallStatus(entity.getPackageId(), "1");
                }

                log.info("批量创建Python包成功并自动安装依赖：{}", cleanedPackages);
            } catch (Exception e) {
                log.error("创建Python包后自动安装依赖失败：{}", e.getMessage(), e);
                throw new ServiceException("Python安装依赖失败,请检查后重新安装");
            }
        }
        return result;
    }

    /**
     * 修改Python包管理
     */
    @Override
    public Boolean updateByBo(SysPythonPackageBo bo) {
        SysPythonPackage update = MapstructUtils.convert(bo, SysPythonPackage.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysPythonPackage entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除Python包管理信息
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验包名是否唯一
     */
    @Override
    public boolean checkPackageNameUnique(SysPythonPackageBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getPackageName, bo.getPackageName())
            .eq(SysPythonPackage::getPackageVersion, bo.getPackageVersion())
            .eq(SysPythonPackage::getUpdateBy, LoginHelper.getLoginUser().getUserId()));
        return !exist;
    }

    /**
     * 根据包名查询Python包管理
     */
    @Override
    public SysPythonPackageVo queryByPackageName(String packageName) {
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getPackageName, packageName));
    }

    /**
     * 根据安装状态查询Python包管理列表
     */
    @Override
    public List<SysPythonPackageVo> queryByInstallStatus(String isInstalled) {
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysPythonPackage>()
            .eq(SysPythonPackage::getIsInstalled, isInstalled));
    }

    /**
     * 更新包安装状态
     */
    @Override
    public Boolean updateInstallStatus(Long packageId, String isInstalled) {

        return baseMapper.update(null, new LambdaUpdateWrapper<SysPythonPackage>()
            .set(SysPythonPackage::getIsInstalled, isInstalled)
            .eq(SysPythonPackage::getPackageId, packageId)) > 0;
    }

    /**
     * 安装Python包
     */
    @Override
    public String installPackages(String packages) {
        String cleanedPackages = cleanPackages(packages);
        execPip("install", cleanedPackages);
        return "成功安装：" + cleanedPackages;
    }

    /**
     * 卸载Python包
     */
    @Override
    public String uninstallPackages(String packages) {
        String cleanedPackages = cleanPackages(packages);
        execPip("uninstall", "-y", cleanedPackages);
        return "成功卸载：" + cleanedPackages;
    }

    /**
     * 根据包ID卸载Python包
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uninstallPackageById(Long packageId) {
        // 查询包信息
        SysPythonPackageVo packageVo = queryById(packageId);
        if (packageVo == null) {
            throw new ServiceException("包不存在，ID: " + packageId);
        }

        if (baseMapper.deleteBatchIds(List.of(packageId)) > 0) {
            return String.format("成功卸载包 %s (版本: %s)", packageVo.getPackageName(), packageVo.getPackageVersion());
        }

        throw new BaseException(String.format("卸载包 %s (版本: %s)失败", packageVo.getPackageName(), packageVo.getPackageVersion()));
    }

    /**
     * 批量根据包ID卸载Python包
     */
    @Override
    public String uninstallPackagesByIds(List<Long> packageIds) {
        StringBuilder result = new StringBuilder();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (Long packageId : packageIds) {
            try {
                String uninstallResult = uninstallPackageById(packageId);
                result.append(uninstallResult).append("; ");
                if (uninstallResult.contains("成功卸载")) {
                    successCount++;
                } else {
                    skipCount++;
                }
            } catch (Exception e) {
                log.error("卸载包失败，ID: {}, 错误: {}", packageId, e.getMessage());
                result.append("卸载包ID ").append(packageId).append(" 失败: ").append(e.getMessage()).append("; ");
                errorCount++;
            }
        }

        return String.format("批量卸载完成，成功: %d个，跳过: %d个，失败: %d个。详情: %s",
            successCount, skipCount, errorCount, result.toString());
    }

    /**
     * 测试连接
     */
    @Override
    public String testConnection() {
        if (pythonProperties.isRemoteMode()) {
            // 测试SSH连接
            boolean connected = SshUtil.testConnection(pythonProperties.getRemote());
            if (connected) {
                return "SSH连接测试成功，服务器地址: " + pythonProperties.getRemote().getHost() + ":" + pythonProperties.getRemote().getPort();
            } else {
                return "SSH连接测试失败，请检查配置和网络连接";
            }
        } else {
            // 测试本地环境
            java.io.File virtualenvDir = new java.io.File(pythonProperties.getLocal().getVirtualenvPath());
            java.io.File activateScript = new java.io.File(pythonProperties.getLocal().getActivateScript());

            if (!virtualenvDir.exists()) {
                return "本地虚拟环境目录不存在: " + pythonProperties.getLocal().getVirtualenvPath();
            }

            if (!activateScript.exists()) {
                return "本地激活脚本不存在: " + pythonProperties.getLocal().getActivateScript();
            }

            return "本地环境测试成功，虚拟环境目录: " + pythonProperties.getLocal().getVirtualenvPath();
        }
    }

    /**
     * 获取已安装包列表
     */
    @Override
    public String getInstalledPackages() {
        String installedList = getInstalledPackagesList();
        if (installedList.isEmpty()) {
            return "暂无已安装的包";
        }
        return installedList;
    }

    /**
     * 同步包安装状态
     */
    @Override
    public String syncPackageStatus() {
        // 获取所有数据库中的包
        List<SysPythonPackageVo> allPackages = queryList(new SysPythonPackageBo());

        // 获取已安装包列表
        String installedPackagesList = getInstalledPackagesList();
        Map<String, String> installedPackagesMap = new HashMap<>();

        // 解析已安装包列表
        if (!installedPackagesList.isEmpty()) {
            String[] lines = installedPackagesList.split("\n");
            for (String line : lines) {
                if (line.contains("==")) {
                    String[] parts = line.split("==");
                    if (parts.length == 2) {
                        installedPackagesMap.put(parts[0].toLowerCase().trim(), parts[1].trim());
                    }
                }
            }
        }

        int syncCount = 0;
        StringBuilder syncDetails = new StringBuilder();

        // 同步每个包的状态
        for (SysPythonPackageVo packageVo : allPackages) {
            String packageName = packageVo.getPackageName().toLowerCase();
            String installedVersion = installedPackagesMap.get(packageName);

            boolean isCurrentlyInstalled = installedVersion != null;
            boolean isMarkedAsInstalled = "1".equals(packageVo.getIsInstalled());

            // 如果状态不一致，则更新
            if (isCurrentlyInstalled != isMarkedAsInstalled) {
                updateInstallStatus(packageVo.getPackageId(), isCurrentlyInstalled ? "1" : "0");
                syncCount++;

                if (isCurrentlyInstalled) {
                    syncDetails.append(String.format("包 %s 已安装（版本: %s），更新状态为已安装; ",
                        packageVo.getPackageName(), installedVersion));
                } else {
                    syncDetails.append(String.format("包 %s 未安装，更新状态为未安装; ",
                        packageVo.getPackageName()));
                }
            }

            // 如果版本不一致，记录警告
            if (isCurrentlyInstalled && !installedVersion.equals(packageVo.getPackageVersion())) {
                syncDetails.append(String.format("警告：包 %s 版本不一致，数据库版本: %s，实际版本: %s; ",
                    packageVo.getPackageName(), packageVo.getPackageVersion(), installedVersion));
            }
        }

        if (syncCount == 0) {
            return "所有包状态已同步，无需更新";
        } else {
            return String.format("同步完成，更新了 %d 个包的状态。详情: %s", syncCount, syncDetails.toString());
        }
    }

    /**
     * 清理包名，确保安全性
     */
    private String cleanPackages(String packages) {
        // 这里可以添加更多的验证逻辑，例如检查包名是否符合规范，避免注入攻击等
        // 示例：简单地移除可能的危险字符

        StringBuilder cleaned = new StringBuilder();
        String[] parts = packages.toLowerCase(Locale.ROOT).split("\\s+");
        for (String part : parts) {
            boolean isValid = true;
            for (char c : part.toCharArray()) {
                boolean charAllowed = false;
                for (String allowed : allowedChars) {
                    if (String.valueOf(c).equals(allowed)) {
                        charAllowed = true;
                        break;
                    }
                }
                if (!charAllowed) {
                    isValid = false;
                    break;
                }
            }
            if (isValid) {
                if (cleaned.length() > 0) {
                    cleaned.append(" ");
                }
                cleaned.append(part);
            }
        }
        if (cleaned.length() < 1) {
            throw new ServiceException("未包含安全且有效的包名");
        }
        return cleaned.toString();
    }

    /**
     * 执行pip命令
     */
    private void execPip(String... cmds) {
        try {
            if (pythonProperties.isRemoteMode()) {
                // 远程模式：通过SSH执行
                executeRemotePip(cmds);
            } else {
                // 本地模式：通过本地进程执行
                executeLocalPip(cmds);
            }
        } catch (Exception e) {
            throw new ServiceException("执行失败: " + e.getMessage());
        }
    }

    /**
     * 本地执行pip命令
     */
    private void executeLocalPip(String... cmds) throws IOException, InterruptedException {
        Process process = buildLocalProcess(cmds);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.append(line).append("\n");
        }
        log.info("本地pip执行输出: {}", lines.toString());
        int exitCode = process.waitFor();

        log.debug("本地pip执行完成，退出码: {}", exitCode);

        if (exitCode != 0) {
            throw new ServiceException("本地pip执行失败，退出码: " + exitCode);
        }
    }

    /**
     * 远程执行pip命令
     */
    private void executeRemotePip(String... cmds) {
        String command = buildRemoteCommand(cmds);
        log.info("远程pip执行命令: {}", command);

        String output = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), command);
        log.info("远程pip执行输出: {}", output);
    }

    /**
     * 构建本地进程
     */
    private Process buildLocalProcess(String... command) throws IOException {
        List<String> cmds = new ArrayList<>();
        cmds.add("bash");
        cmds.add("-c");
        cmds.add("source " + pythonProperties.getLocal().getActivateScript() + " && pip " + String.join(" ", command));
        log.info("本地执行命令: {}", String.join(" ", cmds));
        ProcessBuilder processBuilder = new ProcessBuilder(cmds);
        processBuilder.directory(new java.io.File(pythonProperties.getLocal().getVirtualenvPath()));
        processBuilder.redirectErrorStream(true); // 合并标准错误输出到标准输出
        return processBuilder.start();
    }

    /**
     * 构建远程命令
     */
    private String buildRemoteCommand(String... command) {
        // 先切换到虚拟环境目录，然后激活虚拟环境，最后执行pip命令
        return String.format("cd %s && source %s && pip %s",
            pythonProperties.getRemote().getVirtualenvPath(),
            pythonProperties.getRemote().getActivateScript(),
            String.join(" ", command));
    }

    /**
     * 获取已安装包的版本号
     *
     * @param packageName 包名
     * @return 版本号，如果未安装返回null
     */
    private String getInstalledPackageVersion(String packageName) {
        try {
            String output;
            if (pythonProperties.isRemoteMode()) {
                // 远程模式：通过SSH执行
                String command = String.format("cd %s && source %s && pip show %s | grep Version | cut -d ' ' -f 2",
                    pythonProperties.getRemote().getVirtualenvPath(),
                    pythonProperties.getRemote().getActivateScript(),
                    packageName);
                output = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), command);
            } else {
                // 本地模式：通过本地进程执行
                List<String> cmds = new ArrayList<>();
                cmds.add("bash");
                cmds.add("-c");
                cmds.add(String.format("cd %s && source %s && pip show %s | grep Version | cut -d ' ' -f 2",
                    pythonProperties.getLocal().getVirtualenvPath(),
                    pythonProperties.getLocal().getActivateScript(),
                    packageName));

                ProcessBuilder processBuilder = new ProcessBuilder(cmds);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return null; // 包未安装
                }
                output = result.toString();
            }

            // 清理输出并返回版本号
            String version = output.trim();
            return version.isEmpty() ? null : version;

        } catch (Exception e) {
            log.debug("获取包 {} 版本信息失败: {}", packageName, e.getMessage());
            return null; // 包未安装或获取失败
        }
    }

    /**
     * 获取所有已安装包的列表
     *
     * @return 已安装包的列表，格式为 "包名==版本号"
     */
    private String getInstalledPackagesList() {
        try {
            String output;
            if (pythonProperties.isRemoteMode()) {
                // 远程模式：通过SSH执行
                String command = String.format("cd %s && source %s && pip list --format=freeze",
                    pythonProperties.getRemote().getVirtualenvPath(),
                    pythonProperties.getRemote().getActivateScript());
                output = SshUtil.executeRemoteCommand(pythonProperties.getRemote(), command);
            } else {
                // 本地模式：通过本地进程执行
                List<String> cmds = new ArrayList<>();
                cmds.add("bash");
                cmds.add("-c");
                cmds.add(String.format("cd %s && source %s && pip list --format=freeze",
                    pythonProperties.getLocal().getVirtualenvPath(),
                    pythonProperties.getLocal().getActivateScript()));

                ProcessBuilder processBuilder = new ProcessBuilder(cmds);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }

                process.waitFor();
                output = result.toString();
            }

            return output.trim();

        } catch (Exception e) {
            log.error("获取已安装包列表失败: {}", e.getMessage());
            return "";
        }
    }
}

