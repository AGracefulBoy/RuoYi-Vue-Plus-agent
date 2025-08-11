package org.dromara.system.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 会话分组信息VO
 *
 * @author zhoudashuai
 */
@Data
public class ChatGroupVo {

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 父分组ID
     */
    private Long parentId;

    /**
     * 分组图标
     */
    private String icon;

    /**
     * 分组颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否默认分组（0否 1是）
     */
    private String isDefault;

    /**
     * 分组下的会话数量
     */
    private Integer chatCount;

    /**
     * 分组元数据
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}