package com.admin.hub.app.dto;

import java.util.Map;

public record HealthDependenciesResponse(
        String app,
        Map<String, String> dependencies
) {
}
