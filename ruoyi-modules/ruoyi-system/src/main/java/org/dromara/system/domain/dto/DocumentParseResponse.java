package org.dromara.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Document parse response DTO
 *
 * @author ruoyi
 */
@Data
public class DocumentParseResponse {

    /**
     * Response message
     */
    private String message;

    /**
     * Success count
     */
    @JsonProperty("success_count")
    private Integer successCount;

    /**
     * Success task IDs mapping (file URL -> task ID)
     */
    @JsonProperty("success_task_ids")
    private Map<String, String> successTaskIds;

    /**
     * Error count
     */
    @JsonProperty("error_count")
    private Integer errorCount;

    /**
     * Error messages
     */
    private List<String> errors;
}