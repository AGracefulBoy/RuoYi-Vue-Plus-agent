package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.system.domain.SysToolPackage;

/**
 * 工具包关联Mapper接口
 *
 * @author ruoyi
 */
@Mapper
public interface SysToolPackageMapper extends BaseMapper<SysToolPackage> {
    
    // 所有的查询操作都通过 Service 层使用 Wrapper 实现
    // 不再需要 XML 配置文件
    
}