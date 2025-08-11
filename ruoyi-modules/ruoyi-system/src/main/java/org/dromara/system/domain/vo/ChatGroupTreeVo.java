package org.dromara.system.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 会话分组树形结构VO
 *
 * @author zhoudashuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatGroupTreeVo extends ChatGroupVo {

    /**
     * 子分组列表
     */
    private List<ChatGroupTreeVo> children;

    /**
     * 分组下的会话列表
     */
    private List<ChatSessionVo> chats;

    /**
     * 是否展开
     */
    private Boolean expanded;

    /**
     * 是否有子节点
     */
    private Boolean hasChildren;
}