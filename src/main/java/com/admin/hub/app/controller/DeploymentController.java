package com.admin.hub.app.controller;

import com.admin.hub.app.dto.ApplicationConfiguration;
import com.admin.hub.app.dto.DeploymentResponse;
import com.admin.hub.app.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/deployment")
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final List<SseEmitter> healthEmitters = new CopyOnWriteArrayList<>();
    private final List<SseEmitter> appsStatusEmitters = new CopyOnWriteArrayList<>();

    private static final String HEALTHY_KEY = "healthy";
    private static final String MESSAGE_KEY = "message";

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        boolean isHealthy = deploymentService.healthCheck();
        response.put(HEALTHY_KEY, isHealthy);
        response.put(MESSAGE_KEY, isHealthy ? "Deployer service is healthy" : "Deployer service is unavailable");
        return ResponseEntity.ok(response);
    }

    /**
     * Get all configured applications
     */
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationConfiguration>> getApplications() {
        try {
            List<ApplicationConfiguration> applications = deploymentService.getApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("Error fetching applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Checkout application repository
     */
    @PostMapping("/checkout/{applicationName}")
    public ResponseEntity<DeploymentResponse> checkout(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.checkout(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("checkout")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Build application
     */
    @PostMapping("/build/{applicationName}")
    public ResponseEntity<DeploymentResponse> build(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.build(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("build")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Verify build artifact
     */
    @PostMapping("/verify/{applicationName}")
    public ResponseEntity<DeploymentResponse> verify(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.verify(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("verify")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Deploy application
     */
    @PostMapping("/deploy/{applicationName}")
    public ResponseEntity<DeploymentResponse> deploy(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.deploy(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("deploy")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Restart application service
     */
    @PostMapping("/restart/{applicationName}")
    public ResponseEntity<DeploymentResponse> restart(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.restart(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("restart")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Stop application service
     */
    @PostMapping("/stop/{applicationName}")
    public ResponseEntity<DeploymentResponse> stop(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.stop(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("stop")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get application status
     */
    @GetMapping("/status/{applicationName}")
    public ResponseEntity<DeploymentResponse> getStatus(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.getStatus(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("status")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get application logs
     */
    @GetMapping("/logs/{applicationName}")
    public ResponseEntity<DeploymentResponse> getLogs(
            @PathVariable String applicationName,
            @RequestParam(defaultValue = "1000") Integer lines) {
        try {
            DeploymentResponse response = deploymentService.getLogs(applicationName, lines);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("logs")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Execute full deployment workflow
     */
    @PostMapping("/full-deploy/{applicationName}")
    public ResponseEntity<DeploymentResponse> fullDeploy(@PathVariable String applicationName) {
        try {
            DeploymentResponse response = deploymentService.fullDeploy(applicationName);
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action("full-deploy")
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Execute custom deployment action
     */
    @PostMapping("/execute")
    public ResponseEntity<DeploymentResponse> executeAction(
            @RequestParam String applicationName,
            @RequestParam String action,
            @RequestParam(required = false) Integer lines) {
        try {
            DeploymentResponse response;
            switch (action.toLowerCase()) {
                case "checkout":
                    response = deploymentService.checkout(applicationName);
                    break;
                case "build":
                    response = deploymentService.build(applicationName);
                    break;
                case "verify":
                    response = deploymentService.verify(applicationName);
                    break;
                case "deploy":
                    response = deploymentService.deploy(applicationName);
                    break;
                case "restart":
                    response = deploymentService.restart(applicationName);
                    break;
                case "status":
                    response = deploymentService.getStatus(applicationName);
                    break;
                case "logs":
                    response = deploymentService.getLogs(applicationName, lines != null ? lines : 100);
                    break;
                case "full-deploy":
                    response = deploymentService.fullDeploy(applicationName);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(DeploymentResponse.builder()
                                    .applicationName(applicationName)
                                    .action(action)
                                    .success(false)
                                    .message("Unknown action: " + action)
                                    .build());
            }
            return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DeploymentResponse.builder()
                            .applicationName(applicationName)
                            .action(action)
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Check if application public URL and API URL are healthy
     */
    @GetMapping("/applications/{applicationName}/health")
    public ResponseEntity<Map<String, Object>> checkAppLiveStatus(@PathVariable String applicationName) {
        try {
            Map<String, Object> response = deploymentService.checkApplicationHealthStatus(applicationName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking live status for {}", applicationName, e);
            Map<String, Object> response = new HashMap<>();
            response.put("applicationName", applicationName);
            response.put("applicationUrlLive", false);
            response.put("apiUrlLive", false);
            response.put("healthy", false);
            response.put(MESSAGE_KEY, "Error checking application status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * SSE endpoint for deployment health and app status updates
     */
    @GetMapping(value = "/health/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamHealthAndApps() {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        healthEmitters.add(emitter);
        appsStatusEmitters.add(emitter);

        // Set up callbacks for cleanup
        emitter.onCompletion(() -> {
            healthEmitters.remove(emitter);
            appsStatusEmitters.remove(emitter);
        });
        emitter.onTimeout(() -> {
            healthEmitters.remove(emitter);
            appsStatusEmitters.remove(emitter);
        });
        emitter.onError(throwable -> {
            healthEmitters.remove(emitter);
            appsStatusEmitters.remove(emitter);
            log.debug("SSE connection error", throwable);
        });

        return emitter;
    }

    /**
     * Check if there are active SSE connections from browsers
     */
    public boolean hasActiveConnections() {
        return !healthEmitters.isEmpty() || !appsStatusEmitters.isEmpty();
    }

    /**
     * Method to broadcast health updates to all connected clients
     */
    public void broadcastHealthUpdate(Map<String, Object> healthData) {
        for (SseEmitter emitter : healthEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("health")
                        .data(healthData)
                        .reconnectTime(1000));
            } catch (IOException e) {
                healthEmitters.remove(emitter);
                log.error("Error sending health update to emitter", e);
            }
        }
    }

    /**
     * Method to broadcast app status updates to all connected clients
     */
    public void broadcastAppStatusUpdate(Map<String, Object> appStatusData) {
        for (SseEmitter emitter : appsStatusEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("appStatus")
                        .data(appStatusData)
                        .reconnectTime(1000));
            } catch (IOException e) {
                appsStatusEmitters.remove(emitter);
                log.error("Error sending app status update to emitter", e);
            }
        }
    }
}
