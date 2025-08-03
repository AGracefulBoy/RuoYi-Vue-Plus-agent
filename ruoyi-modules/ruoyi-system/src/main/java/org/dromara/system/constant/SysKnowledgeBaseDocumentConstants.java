package org.dromara.system.constant;

/**
 * 知识库文档管理常量
 *
 * @author ruoyi
 */
public class SysKnowledgeBaseDocumentConstants {

    /**
     * 文档处理状态
     */
    public static final String STATUS_TO_BE_EXECUTED = "to_be_executed";
    public static final String STATUS_DOCUMENT_PARSING = "document_parsing";
    public static final String STATUS_DOCUMENT_CHUNK = "document_chunk";
    public static final String STATUS_DOCUMENT_EMBEDDING = "document_embedding";
    public static final String STATUS_DOCUMENT_FINISHED = "document_finished";
    public static final String STATUS_DOCUMENT_FAIL = "document_fail";


    /**
     * 获取所有有效的状态值
     */
    public static final String[] VALID_STATUS = {
        STATUS_TO_BE_EXECUTED,
        STATUS_DOCUMENT_PARSING,
        STATUS_DOCUMENT_EMBEDDING,
        STATUS_DOCUMENT_FINISHED,
        STATUS_DOCUMENT_FAIL
    };

    /**
     * 默认状态
     */
    public static final String DEFAULT_STATUS = STATUS_TO_BE_EXECUTED;

    /**
     * 校验状态是否有效
     *
     * @param status 状态
     * @return 是否有效
     */
    public static boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }
        for (String validStatus : VALID_STATUS) {
            if (validStatus.equals(status)) {
                return true;
            }
        }
        return false;
    }
}
