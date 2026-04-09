package com.admin.hub.app.service;

import com.admin.hub.app.dto.HealthDependenciesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    public HealthDependenciesResponse getHealth() {
        Map<String, String> dependencies = new LinkedHashMap<>();

        return new HealthDependenciesResponse("healthy", dependencies);
    }
}
