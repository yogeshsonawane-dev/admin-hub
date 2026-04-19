package com.admin.hub.app.controller;

import com.admin.hub.app.dto.RunningService;
import com.admin.hub.app.dto.ServerHealthSummary;
import com.admin.hub.app.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
@Slf4j
public class ServerController {

    private final ServerService serverService;
    private final List<SseEmitter> serverHealthEmitters = new CopyOnWriteArrayList<>();

    /**
     * Get list of running systemd services
     */
    @GetMapping("/services/status")
    public ResponseEntity<List<RunningService>> getRunningServices() {
        try {
            List<RunningService> services = serverService.getRunningServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            log.error("Error fetching running services", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get server health summary
     */
    @GetMapping("/health/summary")
    public ResponseEntity<ServerHealthSummary> getServerHealthSummary() {
        try {
            ServerHealthSummary summary = serverService.getServerHealthSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error fetching server health summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * SSE endpoint for server health and services updates
     */
    @GetMapping(value = "/health/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamServerHealth() {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        serverHealthEmitters.add(emitter);

        // Set up callbacks for cleanup
        emitter.onCompletion(() -> serverHealthEmitters.remove(emitter));
        emitter.onTimeout(() -> serverHealthEmitters.remove(emitter));
        emitter.onError(throwable -> {
            serverHealthEmitters.remove(emitter);
            log.debug("SSE connection error", throwable);
        });

        return emitter;
    }

    /**
     * Check if there are active SSE connections from browsers
     */
    public boolean hasActiveConnections() {
        return !serverHealthEmitters.isEmpty();
    }

    /**
     * Method to broadcast server health updates to all connected clients
     */
    public void broadcastServerHealthUpdate(Map<String, Object> serverHealthData) {
        for (SseEmitter emitter : serverHealthEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name("serverHealth")
                        .data(serverHealthData)
                        .reconnectTime(1000));
            } catch (IOException e) {
                serverHealthEmitters.remove(emitter);
                log.error("Error sending server health update to emitter", e);
            }
        }
    }
}
