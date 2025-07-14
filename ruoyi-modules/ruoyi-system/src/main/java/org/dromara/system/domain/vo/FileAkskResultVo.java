package org.dromara.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FileAkskResultVo {
    private String securityToken;
    private String accessKeySecret;
    private String accessKeyId;
    private String expiration;
    private String requestId;
    private String bucketName;
    private String region;
}
