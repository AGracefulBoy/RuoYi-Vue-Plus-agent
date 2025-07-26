package org.dromara.system.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ToolDto {
    private String name;
    private String desc;
    private List<Parameter> parameters;

    @Data
    @Builder
    public static class Parameter {
        private String name;
        private String desc;
        private String type;
        private Boolean required;
        private List<Parameter> properties;
    }
}
