package org.dromara.common.llm.model.protocol.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseFormatRequest {
    @JsonProperty("type")
    private Type type;

    @JsonProperty("json_schema")
    private JsonSchema jsonSchema;

    @JsonProperty("name")
    private String name;

    @JsonProperty("strict")
    private Boolean strict;

    @JsonProperty("schema")
    private Map<String, Object> schema;

    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public JsonSchema getJsonSchema() {
        return this.jsonSchema;
    }

    public void setJsonSchema(JsonSchema jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getStrict() {
        return this.strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

    public Map<String, Object> getSchema() {
        return this.schema;
    }

    public void setSchema(Map<String, Object> schema) {
        this.schema = schema;
    }

    public ResponseFormatRequest() {
    }

    public ResponseFormatRequest(Type type, JsonSchema jsonSchema, String name, Boolean strict, Map<String, Object> schema) {
        this.type = type;
        this.jsonSchema = jsonSchema;
        this.name = name;
        this.strict = strict;
        this.schema = schema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ResponseFormatRequest that = (ResponseFormatRequest) o;
            return Objects.equals(this.type, that.type) &&
                   Objects.equals(this.jsonSchema, that.jsonSchema) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.strict, that.strict) &&
                   Objects.equals(this.schema, that.schema);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.type, this.jsonSchema, this.name, this.strict, this.schema);
    }

    public String toString() {
        return "ResponseFormatRequest{" +
               "type=" + type +
               ", jsonSchema=" + jsonSchema +
               ", name='" + name + '\'' +
               ", strict=" + strict +
               ", schema=" + schema +
               '}';
    }

    public static final class Builder {
        private Type type;
        private JsonSchema jsonSchema;
        private String name;
        private Boolean strict;
        private Map<String, Object> schema;

        private Builder() {
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder jsonSchema(JsonSchema jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder strict(Boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder schema(Map<String, Object> schema) {
            this.schema = schema;
            return this;
        }

        public ResponseFormatRequest build() {
            return new ResponseFormatRequest(this.type, this.jsonSchema, this.name, this.strict, this.schema);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JsonSchema {
        @JsonProperty("name")
        private String name;

        @JsonProperty("schema")
        private Map<String, Object> schema;

        @JsonProperty("strict")
        private Boolean strict;

        public JsonSchema() {
        }

        public JsonSchema(String name, Map<String, Object> schema, Boolean strict) {
            this.name = name;
            this.schema = schema;
            this.strict = strict;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Object> getSchema() {
            return schema;
        }

        public void setSchema(Map<String, Object> schema) {
            this.schema = schema;
        }

        public Boolean getStrict() {
            return strict;
        }

        public void setStrict(Boolean strict) {
            this.strict = strict;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JsonSchema that = (JsonSchema) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(schema, that.schema) &&
                   Objects.equals(strict, that.strict);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, schema, strict);
        }

        @Override
        public String toString() {
            return "JsonSchema{" +
                   "name='" + name + '\'' +
                   ", schema=" + schema +
                   ", strict=" + strict +
                   '}';
        }
    }

    public static enum Type {
        @JsonProperty("text")
        TEXT,
        @JsonProperty("json_object")
        JSON_OBJECT,
        @JsonProperty("json_schema")
        JSON_SCHEMA;
    }
}
