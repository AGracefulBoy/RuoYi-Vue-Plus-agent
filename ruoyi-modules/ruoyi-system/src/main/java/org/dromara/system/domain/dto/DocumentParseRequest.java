package org.dromara.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document parse request DTO
 *
 * @author ruoyi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseRequest {

    /**
     * File type
     */
    @JsonProperty("file_type")
    private String fileType;

    /**
     * Processing mode
     */
    private Integer mode;

    /**
     * File URL
     */
    @JsonProperty("file_url")
    private String fileUrl;

    /**
     * Enable image recognition
     */
    @JsonProperty("enable_image_recognition")
    private Boolean enableImageRecognition;

    /**
     * Image recognition prompt
     */
    private String prompt;

    private String modelId;
}
