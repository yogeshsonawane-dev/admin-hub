package com.admin.hub.app.service;

import com.admin.hub.app.controller.DeploymentController;
import com.admin.hub.app.controller.ServerController;
import com.admin.hub.app.dto.ApplicationConfiguration;
import com.admin.hub.app.dto.ServerHealthSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SseBroadcastService {

    private final DeploymentService deploymentService;
    private final ServerService serverService;
    private final DeploymentController deploymentController;
    private final ServerController serverController;

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String HEALTHY_KEY = "healthy";
    private static final String MESSAGE_KEY = "message";

    /**
     * Broadcast health and app status updates every 5 seconds
     * Only broadcasts if there are active browser connections
     */
    @Scheduled(fixedRate = 30000)
    public void broadcastHealthAndAppsStatus() {
        // Only broadcast if there are active SSE connections
        if (!deploymentController.hasActiveConnections()) {
            return;
        }

        try {
            // Check deployment service health
            Map<String, Object> healthData = new HashMap<>();
            boolean isHealthy = deploymentService.healthCheck();
            healthData.put(HEALTHY_KEY, isHealthy);
            healthData.put(MESSAGE_KEY, isHealthy ? "Deployer service is healthy" : "Deployer service is unavailable");
            healthData.put(TIMESTAMP_KEY, System.currentTimeMillis());

            deploymentController.broadcastHealthUpdate(healthData);

            // Get and broadcast app live status
            List<ApplicationConfiguration> applications = deploymentService.getApplications();
            Map<String, Object> appStatusData = new HashMap<>();
            Map<String, Map<String, Object>> appStatuses =
                    deploymentService.checkApplicationsHealthStatus(applications);

            appStatusData.put("appStatuses", appStatuses);
            appStatusData.put(TIMESTAMP_KEY, System.currentTimeMillis());

            deploymentController.broadcastAppStatusUpdate(appStatusData);
        } catch (Exception e) {
            log.error("Error broadcasting health and app status updates", e);
        }
    }

    /**
     * Broadcast server health and running services updates every 5 seconds
     * Only broadcasts if there are active browser connections
     */
    @Scheduled(fixedRate = 30000)
    public void broadcastServerHealth() {
        // Only broadcast if there are active SSE connections
        if (!serverController.hasActiveConnections()) {
            return;
        }

        try {
            Map<String, Object> serverHealthData = new HashMap<>();

            // Get server health summary
            ServerHealthSummary healthSummary = serverService.getServerHealthSummary();
            serverHealthData.put("cpuUsage", healthSummary.getCpuUsage());
            serverHealthData.put("memoryUsage", healthSummary.getMemoryUsage());
            serverHealthData.put("diskUsage", healthSummary.getDiskUsage());
            serverHealthData.put("loadAverage", healthSummary.getLoadAverage());
            serverHealthData.put("totalMemory", healthSummary.getTotalMemory());
            serverHealthData.put("usedMemory", healthSummary.getUsedMemory());
            serverHealthData.put("uptime", healthSummary.getUptime());
            serverHealthData.put("usedDisk", healthSummary.getUsedDisk());
            serverHealthData.put("totalDisk", healthSummary.getTotalDisk());
            serverHealthData.put(TIMESTAMP_KEY, System.currentTimeMillis());

            // Get running services
            var runningServices = serverService.getRunningServices();
            serverHealthData.put("runningServices", runningServices);

            serverController.broadcastServerHealthUpdate(serverHealthData);
        } catch (Exception e) {
            log.error("Error broadcasting server health updates", e);
        }
    }
}
