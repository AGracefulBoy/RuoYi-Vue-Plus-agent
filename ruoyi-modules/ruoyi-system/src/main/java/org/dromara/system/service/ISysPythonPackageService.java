package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysPythonPackageBo;
import org.dromara.system.domain.vo.SysPythonPackageVo;

import java.util.Collection;
import java.util.List;

/**
 * Python包管理Service接口
 *
 * @author ruoyi
 */
public interface ISysPythonPackageService {

    /**
     * 查询Python包管理
     *
     * @param packageId 包ID
     * @return Python包管理
     */
    SysPythonPackageVo queryById(Long packageId);

    /**
     * 查询Python包管理列表
     *
     * @param bo        Python包管理
     * @param pageQuery 分页参数
     * @return Python包管理集合
     */
    TableDataInfo<SysPythonPackageVo> queryPageList(SysPythonPackageBo bo, PageQuery pageQuery);

    /**
     * 查询Python包管理列表
     *
     * @param bo Python包管理
     * @return Python包管理集合
     */
    List<SysPythonPackageVo> queryList(SysPythonPackageBo bo);

    /**
     * 新增Python包管理
     *
     * @param bo Python包管理
     * @return 结果
     */
    Boolean insertByBo(SysPythonPackageBo bo);

    /**
     * 批量新增Python包管理
     *
     * @param boList Python包管理列表
     * @return 结果
     */
    Boolean insertBatchByBo(List<SysPythonPackageBo> boList);

    /**
     * 修改Python包管理
     *
     * @param bo Python包管理
     * @return 结果
     */
    Boolean updateByBo(SysPythonPackageBo bo);

    /**
     * 校验并批量删除Python包管理信息
     *
     * @param ids     需要删除的Python包管理主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验包名是否唯一
     *
     * @param bo Python包管理
     * @return 结果
     */
    boolean checkPackageNameUnique(SysPythonPackageBo bo);

    /**
     * 根据包名查询Python包管理
     *
     * @param packageName 包名
     * @return Python包管理
     */
    SysPythonPackageVo queryByPackageName(String packageName);

    /**
     * 根据安装状态查询Python包管理列表
     *
     * @param isInstalled 是否已安装
     * @return Python包管理集合
     */
    List<SysPythonPackageVo> queryByInstallStatus(String isInstalled);

    /**
     * 更新包安装状态
     *
     * @param packageId   包ID
     * @param isInstalled 是否已安装
     * @return 结果
     */
    Boolean updateInstallStatus(Long packageId, String isInstalled);

} 