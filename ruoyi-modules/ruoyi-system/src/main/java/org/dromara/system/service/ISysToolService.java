package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.system.domain.bo.InstallPackageRequestBo;
import org.dromara.system.domain.bo.UninstallPackageRequestBo;
import org.dromara.system.domain.bo.SysToolBo;
import org.dromara.system.domain.bo.ToolDebugRequestBo;
import org.dromara.system.domain.vo.SysToolListVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.domain.vo.PythonPackageVo;

import java.util.Collection;
import java.util.List;

/**
 * 工具管理Service接口
 *
 * @author ruoyi
 */
public interface ISysToolService {

    /**
     * 查询工具管理（包含脚本代码）
     *
     * @param toolId 工具ID
     * @return 工具管理
     */
    SysToolVo queryById(Long toolId);

    /**
     * 查询工具管理列表（不包含脚本代码）
     *
     * @param bo        工具管理
     * @param pageQuery 分页参数
     * @return 工具管理集合
     */
    TableDataInfo<SysToolListVo> queryPageList(SysToolBo bo, PageQuery pageQuery);

    /**
     * 查询工具管理列表（不包含脚本代码）
     *
     * @param bo 工具管理
     * @return 工具管理集合
     */
    List<SysToolListVo> queryList(SysToolBo bo);

    /**
     * 新增工具管理
     *
     * @param bo 工具管理
     * @return 结果
     */
    Boolean insertByBo(SysToolBo bo);

    /**
     * 修改工具管理
     *
     * @param bo 工具管理
     * @return 结果
     */
    Boolean updateByBo(SysToolBo bo);

    /**
     * 校验并批量删除工具管理信息
     *
     * @param ids     需要删除的工具管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验工具名称是否唯一
     *
     * @param bo 工具管理
     * @return 结果
     */
    boolean checkToolNameUnique(SysToolBo bo);


    /**
     * 复制工具管理
     *
     * @param toolId 原工具ID
     * @return 结果
     */
    Boolean copyTool(Long toolId);


    /**
     * 安装工具的Python包
     *
     * @param request 安装请求参数（包含工具ID）
     * @return 安装结果消息
     */
    String installToolPackage(InstallPackageRequestBo request);

    /**
     * 卸载工具的Python包
     *
     * @param request 卸载请求参数（包含工具ID、包名、版本号）
     * @return 卸载结果消息
     */
    String uninstallToolPackage(UninstallPackageRequestBo request);

    /**
     * 在虚拟环境中执行工具脚本（非流式）
     *
     * @param request 执行请求
     * @return 执行结果
     */
    String executeToolScript(ToolDebugRequestBo request);

    /**
     * 在虚拟环境中执行工具脚本（流式）
     *
     * @param request 执行请求
     * @param response HTTP响应对象
     */
    void executeToolScriptStream(ToolDebugRequestBo request, HttpServletResponse response);

    /**
     * 搜索Python包（分页）
     *
     * @param query 搜索关键词
     * @param pageQuery 分页参数
     * @return Python包信息分页列表
     */
    TableDataInfo<PythonPackageVo> searchPythonPackages(String query, PageQuery pageQuery);

}
