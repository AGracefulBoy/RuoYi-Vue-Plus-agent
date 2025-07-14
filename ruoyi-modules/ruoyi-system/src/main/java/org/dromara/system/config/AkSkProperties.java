package org.dromara.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file.aksk")
@Data
public class AkSkProperties {
    private String endpoint;
    private String accessKeyID;
    private String accessKeySecret;
    private String roleArn;
    private String roleSessionName;
    private Long durationSeconds;
    private String bucketName;
    private String region;
}
