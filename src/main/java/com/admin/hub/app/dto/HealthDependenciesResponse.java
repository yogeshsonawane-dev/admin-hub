package com.admin.hub.app.dto;

import java.util.Map;

public record HealthDependenciesResponse(
        String status,
        Map<String, String> dependencies
) {
}
