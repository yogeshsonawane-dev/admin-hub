package com.admin.hub.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationConfiguration {
    private String name;

    @JsonProperty("git_url")
    private String gitUrl;

    private String branch;

    @JsonProperty("build_type")
    private String buildType;

    @JsonProperty("artifact_path")
    private String artifactPath;

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("deploy_path")
    private String deployPath;

    @JsonProperty("application_url")
    private String applicationUrl;

    @JsonProperty("api_health_end_point")
    private String apiHealthEndPoint;

    private String symlink;
}

