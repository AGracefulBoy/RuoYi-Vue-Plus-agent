package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.SysToolPackage;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.system.domain.bo.InstallPackageRequestBo;
import org.dromara.system.domain.bo.UninstallPackageRequestBo;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.bo.ToolDebugRequestBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.domain.vo.PythonPackageVo;
import org.dromara.system.domain.vo.ToolPackageVo;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.mapper.SysToolPackageMapper;
import org.dromara.system.service.ISysToolService;
import org.dromara.system.service.IToolVirtualEnvService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;

/**
 * 工具管理Service业务层处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysToolServiceImpl implements ISysToolService {

    private final SysToolMapper baseMapper;
    private final IToolVirtualEnvService virtualEnvService;
    private final SysToolPackageMapper toolPackageMapper;

    /**
     * 查询工具管理（包含脚本代码和包依赖）
     */
    @Override
    public SysToolVo queryById(Long toolId) {
        return baseMapper.selectVoById(toolId);
    }

    /**
     * 查询工具管理列表（不包含脚本代码）
     */
    @Override
    public TableDataInfo<SysToolListVo> queryPageList(SysToolBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTool> lqw = buildQueryWrapper(bo);
        Page<SysTool> page = pageQuery.build();
        return TableDataInfo.build(baseMapper.selectToolListVoPage(page, lqw));
    }

    /**
     * 查询工具管理列表（不包含脚本代码）
     */
    @Override
    public List<SysToolListVo> queryList(SysToolBo bo) {
        LambdaQueryWrapper<SysTool> lqw = buildQueryWrapper(bo);
        return baseMapper.selectToolListVo(lqw);
    }

    private LambdaQueryWrapper<SysTool> buildQueryWrapper(SysToolBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        // 注意：del_flag 的过滤已经在 Mapper 的 SQL 中处理，这里不再添加
        lqw.like(StringUtils.isNotBlank(bo.getToolName()), SysTool::getToolName, bo.getToolName());
        lqw.like(StringUtils.isNotBlank(bo.getToolDesc()), SysTool::getToolDesc, bo.getToolDesc());
        lqw.eq(StringUtils.isNotBlank(bo.getFunctionName()), SysTool::getFunctionName, bo.getFunctionName());
        lqw.eq(StringUtils.isNotBlank(bo.getToolType()), SysTool::getToolType, bo.getToolType());
        lqw.eq(StringUtils.isNotBlank(bo.getIsStream()), SysTool::getIsStream, bo.getIsStream());
        lqw.eq(StringUtils.isNotBlank(bo.getToolStatus()), SysTool::getToolStatus, bo.getToolStatus());
        lqw.between(params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            SysTool::getCreateTime, params.get("beginCreateTime"), params.get("endCreateTime"));
        return lqw;
    }

    /**
     * 新增工具管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SysToolBo bo) {
        SysTool add = MapstructUtils.convert(bo, SysTool.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setToolId(add.getToolId());

            // 为script类型的工具创建虚拟环境
            if ("script".equals(bo.getToolType())) {
                try {
                    String pythonVersion = StringUtils.isNotBlank(bo.getPythonVersion())
                        ? bo.getPythonVersion() : "3.9";
                    boolean venvCreated = virtualEnvService.createToolVirtualEnv(add.getToolId(), pythonVersion);
                    if (!venvCreated) {
                        log.warn("工具虚拟环境创建失败，但工具已创建: toolId={}", add.getToolId());
                    }
                } catch (Exception e) {
                    log.error("创建工具虚拟环境失败: toolId={}", add.getToolId(), e);
                    // 不影响工具创建，继续执行
                }
            }
        }
        return flag;
    }

    /**
     * 修改工具管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SysToolBo bo) {
        SysTool update = MapstructUtils.convert(bo, SysTool.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysTool entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除工具管理
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }

        // 删除工具前，先删除对应的虚拟环境
        for (Long toolId : ids) {
            try {
                virtualEnvService.deleteToolVirtualEnv(toolId);
            } catch (Exception e) {
                log.error("删除工具虚拟环境失败: toolId={}", toolId, e);
                // 继续删除其他环境
            }
        }

        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 校验工具名称是否唯一
     */
    @Override
    public boolean checkToolNameUnique(SysToolBo bo) {
        LambdaQueryWrapper<SysTool> lqw = Wrappers.lambdaQuery();
        lqw.eq(SysTool::getToolName, bo.getToolName());
        lqw.ne(ObjectUtil.isNotNull(bo.getToolId()), SysTool::getToolId, bo.getToolId());
        return baseMapper.selectCount(lqw) == 0;
    }
    /**
     * 复制工具管理
     */
    @Override
    public Boolean copyTool(Long toolId) {
        // 查询原工具信息
        SysToolVo originalTool = baseMapper.selectVoById(toolId);
        if (ObjectUtil.isNull(originalTool)) {
            return false;
        }

        // 手动创建新的业务对象并复制属性
        SysToolBo copyToolBo = new SysToolBo();

        // 复制基本属性
        copyToolBo.setToolName(originalTool.getToolName());
        copyToolBo.setToolDesc(originalTool.getToolDesc());
        copyToolBo.setFunctionName(originalTool.getFunctionName());
        copyToolBo.setToolType(originalTool.getToolType());
        copyToolBo.setIsStream(originalTool.getIsStream());
        copyToolBo.setScriptCode(originalTool.getScriptCode());
        copyToolBo.setToolStatus(originalTool.getToolStatus());
        copyToolBo.setRemark(originalTool.getRemark());

        // 生成新的工具名称：原工具名 + 副本 + 时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        String newToolName = originalTool.getToolName() + "副本" + timestamp;
        copyToolBo.setToolName(newToolName);

        // 清空ID，让系统自动生成新的ID
        copyToolBo.setToolId(null);

        // 设置创建时间和更新时间为空，让系统自动填充
        copyToolBo.setCreateTime(null);
        copyToolBo.setUpdateTime(null);

        // 插入新的工具记录
        return insertByBo(copyToolBo);
    }



    /**
     * 安装工具的Python包
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String installToolPackage(InstallPackageRequestBo request) {
        Long toolId = request.getToolId();
        String packageName = request.getPackageName();
        String packageVersion = request.getPackageVersion();

        // 1. 验证工具是否存在
        SysTool tool = baseMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在: " + toolId);
        }

        // 2. 检查虚拟环境状态，首次安装时创建
        if (StringUtils.isBlank(tool.getVenvPath()) || !"ready".equals(tool.getVenvStatus())) {
            log.info("工具 {} 首次安装包，创建虚拟环境", toolId);

            // 获取Python版本，默认3.9
            String pythonVersion = StringUtils.isNotBlank(tool.getPythonVersion())
                ? tool.getPythonVersion() : "3.9";

            // 创建虚拟环境
            boolean created = virtualEnvService.createToolVirtualEnv(toolId, pythonVersion);
            if (!created) {
                throw new ServiceException("虚拟环境创建失败，无法安装包");
            }

            // 重新查询工具信息，获取更新后的虚拟环境路径
            tool = baseMapper.selectById(toolId);
        }

        // 3. 检查包是否已安装（避免重复）
        LambdaQueryWrapper<SysToolPackage> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysToolPackage::getToolId, toolId)
                    .eq(SysToolPackage::getPackageName, packageName);

        SysToolPackage existingPackage = toolPackageMapper.selectOne(queryWrapper);
        if (existingPackage != null) {
            // 如果包已存在，检查是否需要更新版本
            if (StringUtils.isNotBlank(packageVersion) &&
                !packageVersion.equals(existingPackage.getPackageVersion())) {
                log.info("更新包版本: {} from {} to {}", packageName,
                    existingPackage.getPackageVersion(), packageVersion);
            } else {
                return "包已安装: " + packageName +
                    (StringUtils.isNotBlank(existingPackage.getPackageVersion())
                        ? "==" + existingPackage.getPackageVersion() : "");
            }
        }

        // 4. 构建包安装命令（包含版本号）
        String packageSpec = packageName;
        if (StringUtils.isNotBlank(packageVersion)) {
            packageSpec += "==" + packageVersion;
        }

        // 5. 在虚拟环境中安装包
        log.info("在工具 {} 的虚拟环境中安装包: {}", toolId, packageSpec);
        String installResult = virtualEnvService.installPackagesInToolEnv(
            toolId,
            Collections.singletonList(packageSpec)
        );

        // 6. 记录或更新包信息到数据库
        if (existingPackage != null) {
            // 更新现有记录
            existingPackage.setPackageVersion(packageVersion);
            existingPackage.setUpdateBy(LoginHelper.getUserId());
            existingPackage.setUpdateTime(new Date());
            toolPackageMapper.updateById(existingPackage);
        } else {
            // 插入新记录
            SysToolPackage toolPackage = new SysToolPackage();
            toolPackage.setToolId(toolId);
            toolPackage.setPackageName(packageName);
            toolPackage.setPackageVersion(StringUtils.isNotBlank(packageVersion) ? packageVersion : "latest");
            // packageId 是自增的，不需要设置
            toolPackageMapper.insert(toolPackage);
        }

        // 7. 返回安装结果
        return "包安装成功: " + packageSpec + "\n" + installResult;
    }

    /**
     * 卸载工具的Python包
     */
    @Override
    public String uninstallToolPackage(UninstallPackageRequestBo request) {
        Long toolId = request.getToolId();
        String packageName = request.getPackageName();
        String packageVersion = request.getPackageVersion();

        // 1. 验证工具是否存在
        SysTool tool = baseMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在: " + toolId);
        }

        // 2. 验证虚拟环境状态（必须已创建且ready）
        if (StringUtils.isBlank(tool.getVenvPath()) || !"ready".equals(tool.getVenvStatus())) {
            throw new ServiceException("虚拟环境未就绪，无法卸载包");
        }

        // 3. 查询包安装记录
        LambdaQueryWrapper<SysToolPackage> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysToolPackage::getToolId, toolId)
                    .eq(SysToolPackage::getPackageName, packageName);

        // 如果指定了版本号，添加版本条件
        if (StringUtils.isNotBlank(packageVersion)) {
            queryWrapper.eq(SysToolPackage::getPackageVersion, packageVersion);
        }

        SysToolPackage installedPackage = toolPackageMapper.selectOne(queryWrapper);
        if (installedPackage == null) {
            String versionInfo = StringUtils.isNotBlank(packageVersion) ?
                "==" + packageVersion : "";
            return "包未安装: " + packageName + versionInfo;
        }

        // 4. 构建卸载命令
        String packageSpec = packageName;
        String actualVersion = installedPackage.getPackageVersion();

        // 如果数据库中有版本信息，使用精确版本进行卸载
        if (StringUtils.isNotBlank(actualVersion) && !"latest".equals(actualVersion)) {
            packageSpec += "==" + actualVersion;
        }

        // 5. 在虚拟环境中卸载包
        log.info("在工具 {} 的虚拟环境中卸载包: {}", toolId, packageSpec);
        String uninstallResult = virtualEnvService.uninstallPackagesInToolEnv(
            toolId,
            Collections.singletonList(packageName) // pip uninstall只需要包名，不需要版本号
        );

        // 6. 从数据库删除包记录
        toolPackageMapper.deleteById(installedPackage.getPackageId());

        // 7. 返回卸载结果
        return "包卸载成功: " + packageSpec + "\n" + uninstallResult;
    }

    /**
     * 查询工具已安装的Python包
     */
    @Override
    public List<ToolPackageVo> getInstalledPackagesByToolId(Long toolId) {
        // 1. 验证工具是否存在
        SysTool tool = baseMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在: " + toolId);
        }
        
        // 2. 查询该工具的所有已安装包
        LambdaQueryWrapper<SysToolPackage> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(SysToolPackage::getToolId, toolId)
                    .orderByAsc(SysToolPackage::getPackageName);
        
        List<SysToolPackage> packages = toolPackageMapper.selectList(queryWrapper);
        
        // 3. 手动转换为VO对象
        List<ToolPackageVo> result = new ArrayList<>();
        for (SysToolPackage pkg : packages) {
            ToolPackageVo vo = new ToolPackageVo();
            vo.setPackageId(pkg.getPackageId());
            vo.setPackageName(pkg.getPackageName());
            vo.setPackageVersion(pkg.getPackageVersion());
            vo.setCreateTime(pkg.getCreateTime());
            vo.setUpdateTime(pkg.getUpdateTime());
            result.add(vo);
        }
        
        return result;
    }

    /**
     * 在虚拟环境中执行工具脚本（非流式）
     */
    @Override
    public String executeToolScript(ToolDebugRequestBo request) {
        Long toolId = request.getToolId();

        // 1. 验证工具存在并获取脚本代码
        SysTool tool = baseMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在: " + toolId);
        }

        if (StringUtils.isBlank(tool.getScriptCode())) {
            throw new ServiceException("工具没有脚本代码");
        }

        if (StringUtils.isBlank(tool.getFunctionName())) {
            throw new ServiceException("工具没有指定函数名");
        }

        // 2. 检查并创建虚拟环境
        // 如果虚拟环境路径为空或状态不是ready，需要创建虚拟环境
        if (StringUtils.isBlank(tool.getVenvPath()) || !"ready".equals(tool.getVenvStatus())) {
            log.info("工具 {} 虚拟环境未就绪（状态: {}, 路径: {}），开始创建虚拟环境",
                toolId, tool.getVenvStatus(), tool.getVenvPath());

            // 获取Python版本，默认使用3.9
            String pythonVersion = StringUtils.isNotBlank(tool.getPythonVersion())
                ? tool.getPythonVersion() : "3.9";

            log.info("使用Python版本 {} 创建虚拟环境", pythonVersion);

            // 创建虚拟环境
            boolean created = virtualEnvService.createToolVirtualEnv(toolId, pythonVersion);
            if (!created) {
                throw new ServiceException("虚拟环境创建失败，无法执行脚本");
            }

            // 重新获取工具信息以获取更新后的虚拟环境路径
            tool = baseMapper.selectById(toolId);
            log.info("虚拟环境创建成功，路径: {}, 状态: {}", tool.getVenvPath(), tool.getVenvStatus());
        } else {
            log.info("工具 {} 虚拟环境已就绪，路径: {}", toolId, tool.getVenvPath());
        }

        // 3. 使用虚拟环境执行脚本
        try {
            String result = virtualEnvService.executeInToolEnvSync(
                toolId,
                tool.getScriptCode(),
                tool.getFunctionName(),
                request.getParams()
            );

            // 4. 记录执行日志
            log.info("工具 {} 脚本执行成功，函数: {}", toolId, tool.getFunctionName());

            return result;
        } catch (Exception e) {
            log.error("工具 {} 脚本执行失败", toolId, e);
            throw new ServiceException("脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 在虚拟环境中执行工具脚本（流式）
     */
    @Override
    public void executeToolScriptStream(ToolDebugRequestBo request, HttpServletResponse response) {
        Long toolId = request.getToolId();

        // 1. 验证工具
        SysTool tool = baseMapper.selectById(toolId);
        if (tool == null) {
            throw new ServiceException("工具不存在: " + toolId);
        }

        if (StringUtils.isBlank(tool.getScriptCode())) {
            throw new ServiceException("工具没有脚本代码");
        }

        if (StringUtils.isBlank(tool.getFunctionName())) {
            throw new ServiceException("工具没有指定函数名");
        }

        // 2. 检查并创建虚拟环境
        // 如果虚拟环境路径为空或状态不是ready，需要创建虚拟环境
        if (StringUtils.isBlank(tool.getVenvPath()) || !"ready".equals(tool.getVenvStatus())) {
            log.info("工具 {} 虚拟环境未就绪（状态: {}, 路径: {}），开始创建虚拟环境",
                toolId, tool.getVenvStatus(), tool.getVenvPath());

            // 获取Python版本，默认使用3.9
            String pythonVersion = StringUtils.isNotBlank(tool.getPythonVersion())
                ? tool.getPythonVersion() : "3.9";

            log.info("使用Python版本 {} 创建虚拟环境", pythonVersion);

            // 创建虚拟环境
            boolean created = virtualEnvService.createToolVirtualEnv(toolId, pythonVersion);
            if (!created) {
                throw new ServiceException("虚拟环境创建失败，无法执行流式脚本");
            }

            // 重新获取工具信息以获取更新后的虚拟环境路径
            tool = baseMapper.selectById(toolId);
            log.info("虚拟环境创建成功，路径: {}, 状态: {}", tool.getVenvPath(), tool.getVenvStatus());
        } else {
            log.info("工具 {} 虚拟环境已就绪，路径: {}", toolId, tool.getVenvPath());
        }

        // 3. 使用虚拟环境执行脚本（流式）
        try {
            virtualEnvService.executeInToolEnv(
                toolId,
                tool.getScriptCode(),
                tool.getFunctionName(),
                request.getParams(),
                response
            );

            log.info("工具 {} 流式脚本执行完成", toolId);
        } catch (Exception e) {
            log.error("工具 {} 流式脚本执行失败", toolId, e);
            throw new ServiceException("流式执行失败: " + e.getMessage());
        }
    }

    /**
     * Libraries.io API密钥
     */
    private static final String LIBRARIES_IO_API_KEY = "b943908d69f22be15f0c473296373e47";

    /**
     * 搜索Python包（分页）- 使用Libraries.io API
     */
    @Override
    public TableDataInfo<PythonPackageVo> searchPythonPackages(String query, PageQuery pageQuery) {
        List<PythonPackageVo> results = new ArrayList<>();

        if (StringUtils.isBlank(query)) {
            return TableDataInfo.build(results);
        }

        try {
            // 创建HttpClient
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            // 构建Libraries.io搜索API请求URL - 实现模糊匹配
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            int pageSize = pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 10;
            int pageNum = pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1;

            String url = "https://libraries.io/api/search?q=" + encodedQuery +
                        "&platforms=PyPI&per_page=" + pageSize +
                        "&page=" + pageNum +
                        "&api_key=" + LIBRARIES_IO_API_KEY;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

            // 发送请求
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 解析Libraries.io API的JSON响应
                JSONArray jsonArray = JSONUtil.parseArray(response.body());

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject packageJson = jsonArray.getJSONObject(i);

                    PythonPackageVo packageVo = new PythonPackageVo();
                    packageVo.setPackageName(packageJson.getStr("name", ""));
                    packageVo.setPackageVersion(packageJson.getStr("latest_stable_release_number", ""));
                    packageVo.setPackageDesc(packageJson.getStr("description", ""));

                    results.add(packageVo);
                }

                log.info("成功搜索Python包，关键词: {}, 结果数: {}", query, results.size());
            } else {
                log.warn("Libraries.io API请求失败，状态码: {}, 响应: {}",
                    response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("搜索Python包失败: {}", query, e);
        }

        return TableDataInfo.build(results);
    }

}
