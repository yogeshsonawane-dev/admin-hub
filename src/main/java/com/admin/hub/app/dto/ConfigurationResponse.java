package com.admin.hub.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationResponse {
    private boolean success;
    private Map<String, Object> data;

    // Helper method to extract applications
    @JsonProperty("data")
    private void setDataAndExtract(Map<String, Object> data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ApplicationConfiguration> getApplicationsMap() {
        if (data != null && data.containsKey("applications")) {
            Map<String, Object> appsObj = (Map<String, Object>) data.get("applications");
            Map<String, ApplicationConfiguration> result = new HashMap<>();

            if (appsObj != null) {
                appsObj.forEach((name, config) -> {
                    if (config instanceof Map) {
                        Map<String, Object> configMap = (Map<String, Object>) config;
                        ApplicationConfiguration appConfig = ApplicationConfiguration.builder()
                                .name(name)
                                .gitUrl((String) configMap.get("git_url"))
                                .branch((String) configMap.get("branch"))
                                .buildType((String) configMap.get("build_type"))
                                .artifactPath((String) configMap.get("artifact_path"))
                                .serviceName((String) configMap.get("service_name"))
                                .deployPath((String) configMap.get("deploy_path"))
                                .symlink((String) configMap.get("symlink"))
                                .applicationUrl((String) configMap.get("application_url"))
                                .apiHealthEndPoint((String) configMap.get("api_health_end_point"))
                                .build();
                        result.put(name, appConfig);
                    }
                });
            }
            return result;
        }
        return new HashMap<>();
    }
}

