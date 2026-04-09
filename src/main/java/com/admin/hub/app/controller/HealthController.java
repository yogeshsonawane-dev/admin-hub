package com.admin.hub.app.controller;

import com.admin.hub.app.dto.HealthDependenciesResponse;
import com.admin.hub.app.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/health")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    public ResponseEntity<HealthDependenciesResponse> getHealth() {
        return ResponseEntity.ok(healthService.getHealth());
    }
}
