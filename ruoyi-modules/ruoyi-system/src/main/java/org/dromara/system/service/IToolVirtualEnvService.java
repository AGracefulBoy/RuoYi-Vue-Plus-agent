package org.dromara.system.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.system.domain.vo.ToolVenvStatusVo;

import java.util.List;
import java.util.Map;

/**
 * 工具虚拟环境管理Service接口
 *
 * @author ruoyi
 */
public interface IToolVirtualEnvService {

    /**
     * 为工具创建独立虚拟环境
     *
     * @param toolId       工具ID
     * @param pythonVersion Python版本 (如: 3.9, 3.10)
     * @return 创建是否成功
     */
    boolean createToolVirtualEnv(Long toolId, String pythonVersion);

    /**
     * 在工具虚拟环境中安装Python包
     *
     * @param toolId   工具ID
     * @param packages 要安装的包列表 (格式: package==version)
     * @return 安装结果消息
     */
    String installPackagesInToolEnv(Long toolId, List<String> packages);

    /**
     * 在工具虚拟环境中卸载Python包
     *
     * @param toolId   工具ID
     * @param packages 要卸载的包列表
     * @return 卸载结果消息
     */
    String uninstallPackagesInToolEnv(Long toolId, List<String> packages);

    /**
     * 在工具虚拟环境中执行Python代码
     *
     * @param toolId       工具ID
     * @param code         Python代码
     * @param functionName 函数名称
     * @param params       函数参数
     * @param response     HTTP响应对象（用于流式输出）
     */
    void executeInToolEnv(Long toolId, String code, String functionName,
                         Map<String, Object> params, HttpServletResponse response);

    /**
     * 在工具虚拟环境中执行Python代码（非流式）
     *
     * @param toolId       工具ID
     * @param code         Python代码
     * @param functionName 函数名称
     * @param params       函数参数
     * @return 执行结果
     */
    String executeInToolEnvSync(Long toolId, String code, String functionName,
                                Map<String, Object> params);

    /**
     * 删除工具虚拟环境
     *
     * @param toolId 工具ID
     * @return 删除是否成功
     */
    boolean deleteToolVirtualEnv(Long toolId);

    /**
     * 检查工具虚拟环境状态
     *
     * @param toolId 工具ID
     * @return 环境状态信息
     */
    ToolVenvStatusVo checkToolEnvStatus(Long toolId);

    /**
     * 重建工具虚拟环境
     *
     * @param toolId 工具ID
     * @return 重建是否成功
     */
    boolean rebuildToolVirtualEnv(Long toolId);

    /**
     * 获取工具虚拟环境的已安装包列表
     *
     * @param toolId 工具ID
     * @return 已安装包列表 (格式: package==version)
     */
    List<String> getInstalledPackagesInToolEnv(Long toolId);

    /**
     * 升级工具虚拟环境中的pip
     *
     * @param toolId 工具ID
     * @return 升级结果消息
     */
    String upgradePipInToolEnv(Long toolId);

    /**
     * 生成工具的requirements.txt文件
     *
     * @param toolId 工具ID
     * @return requirements内容
     */
    String generateRequirements(Long toolId);

    /**
     * 从requirements.txt安装依赖
     *
     * @param toolId      工具ID
     * @param requirements requirements内容
     * @return 安装结果
     */
    String installFromRequirements(Long toolId, String requirements);

    /**
     * 批量创建虚拟环境
     *
     * @param toolIds      工具ID列表
     * @param pythonVersion Python版本
     * @return 创建结果Map<工具ID, 是否成功>
     */
    Map<Long, Boolean> batchCreateVirtualEnvs(List<Long> toolIds, String pythonVersion);

    /**
     * 获取虚拟环境磁盘使用情况
     *
     * @param toolId 工具ID
     * @return 磁盘使用大小（字节）
     */
    Long getVenvDiskUsage(Long toolId);

    /**
     * 清理虚拟环境缓存
     *
     * @param toolId 工具ID
     * @return 清理结果消息
     */
    String cleanVenvCache(Long toolId);

    /**
     * 验证Python版本是否可用
     *
     * @param pythonVersion Python版本
     * @return 是否可用
     */
    boolean validatePythonVersion(String pythonVersion);

    /**
     * 获取可用的Python版本列表
     *
     * @return Python版本列表
     */
    List<String> getAvailablePythonVersions();
}