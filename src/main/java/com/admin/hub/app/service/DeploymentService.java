package com.admin.hub.app.service;

import com.admin.hub.app.config.DeployerProperties;
import com.admin.hub.app.dto.ApplicationConfiguration;
import com.admin.hub.app.dto.ConfigurationResponse;
import com.admin.hub.app.dto.DeploymentResponse;
import com.admin.hub.app.dto.LogsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentService {

    private final RestTemplate restTemplate;
    private final DeployerProperties deployerProperties;

    /**
     * Get configuration and available applications
     */
    public List<ApplicationConfiguration> getApplications() {
        try {
            String url = deployerProperties.getBaseUrl() + "/api/v1/configuration";
            HttpEntity<String> entity = createRequestEntity();

            log.info("Fetching applications from: {}", url);
            ResponseEntity<ConfigurationResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ConfigurationResponse.class
            );

            if (response.getBody() != null) {
                Map<String, ApplicationConfiguration> appsMap = response.getBody().getApplicationsMap();
                if (appsMap != null && !appsMap.isEmpty()) {
                    return new ArrayList<>(appsMap.values());
                }
            }
            return new ArrayList<>();
        } catch (RestClientException e) {
            log.error("Error fetching applications configuration", e);
            throw new IllegalStateException("Failed to fetch applications from deployer service: " + e.getMessage(), e);
        }
    }

    /**
     * Checkout/clone or update repository
     */
    public DeploymentResponse checkout(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/repository/checkout/" + applicationName,
                HttpMethod.POST,
                "checkout"
        );
    }

    /**
     * Build the application
     */
    public DeploymentResponse build(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/build/application/" + applicationName,
                HttpMethod.POST,
                "build"
        );
    }

    /**
     * Verify the build artifact
     */
    public DeploymentResponse verify(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/artifact/verify/" + applicationName,
                HttpMethod.POST,
                "verify"
        );
    }

    /**
     * Deploy the artifact
     */
    public DeploymentResponse deploy(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/deployment/deploy/" + applicationName,
                HttpMethod.POST,
                "deploy"
        );
    }

    /**
     * Restart the application service
     */
    public DeploymentResponse restart(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/application/restart/" + applicationName,
                HttpMethod.POST,
                "restart"
        );
    }

    /**
     * Stop the application service
     */
    public DeploymentResponse stop(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/application/stop/" + applicationName,
                HttpMethod.POST,
                "stop"
        );
    }

    /**
     * Get application status
     */
    public DeploymentResponse getStatus(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/application/status/" + applicationName,
                HttpMethod.GET,
                "status"
        );
    }

    /**
     * Get application logs
     */
    public DeploymentResponse getLogs(String applicationName, Integer lines) {
        try {
            String endpoint = "/api/v1/application/logs/" + applicationName;
            if (lines != null && lines > 0) {
                endpoint += "?lines=" + Math.min(lines, 10000);
            }

            String url = deployerProperties.getBaseUrl() + endpoint;
            HttpEntity<String> entity = createRequestEntity();

            log.info("Fetching logs for {} from: {}", applicationName, url);
            ResponseEntity<LogsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, LogsResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                LogsResponse logsResponse = response.getBody();
                String logsContent = logsResponse.getData() != null
                        ? logsResponse.getData().getCombinedLogs()
                        : "No logs available";

                return DeploymentResponse.builder()
                        .applicationName(applicationName)
                        .action("logs")
                        .success(logsResponse.isSuccess())
                        .message("Logs retrieved successfully")
                        .logs(logsContent)
                        .data(logsResponse.getData())
                        .build();
            } else {
                return DeploymentResponse.builder()
                        .applicationName(applicationName)
                        .action("logs")
                        .success(false)
                        .message("Failed to fetch logs with status: " + response.getStatusCode())
                        .build();
            }
        } catch (RestClientException e) {
            log.error("Error fetching logs for {}: {}", applicationName, e.getMessage(), e);
            return DeploymentResponse.builder()
                    .applicationName(applicationName)
                    .action("logs")
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Full deployment workflow: checkout → build → verify → deploy → restart → status
     */
    public DeploymentResponse fullDeploy(String applicationName) {
        return executeDeploymentAction(
                applicationName,
                "/api/v1/deployment/workflow/full-deploy/" + applicationName,
                HttpMethod.POST,
                "full-deploy"
        );
    }

    /**
     * Health check
     */
    public boolean healthCheck() {
        try {
            String url = deployerProperties.getBaseUrl() + "/health";
            HttpEntity<String> entity = createRequestEntity();
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Health check failed", e);
            return false;
        }
    }

    /**
     * Check both public app URL and API URL health for an application
     */
    public Map<String, Object> checkApplicationHealthStatus(String applicationName) {
        ApplicationConfiguration app = getApplicationByName(applicationName);
        return buildHealthStatus(applicationName, app);
    }

    /**
     * Check both public app URL and API URL health for all applications
     */
    public Map<String, Map<String, Object>> checkApplicationsHealthStatus(List<ApplicationConfiguration> applications) {
        Map<String, Map<String, Object>> appStatuses = new HashMap<>();

        for (ApplicationConfiguration app : applications) {
            if (app != null && app.getName() != null) {
                appStatuses.put(app.getName(), buildHealthStatus(app.getName(), app));
            }
        }

        return appStatuses;
    }

    private ApplicationConfiguration getApplicationByName(String applicationName) {
        try {
            List<ApplicationConfiguration> apps = getApplications();
            return apps.stream()
                    .filter(a -> a.getName().equals(applicationName))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Unexpected error loading application config for {}", applicationName, e);
            return null;
        }
    }

    private Map<String, Object> buildHealthStatus(String applicationName, ApplicationConfiguration app) {
        Map<String, Object> response = new HashMap<>();
        boolean applicationUrlLive = checkApplicationUrlLiveStatus(app);
        boolean apiUrlLive = checkApiUrlLiveStatus(app);
        boolean healthy = applicationUrlLive && apiUrlLive;

        response.put("applicationName", applicationName);
        response.put("applicationUrlLive", applicationUrlLive);
        response.put("apiUrlLive", apiUrlLive);
        response.put("healthy", healthy);
        response.put("message", healthy
                ? "Application URL and API URL are healthy"
                : "Application URL or API URL is not responding");
        return response;
    }

    private boolean checkApplicationUrlLiveStatus(ApplicationConfiguration app) {
        if (app == null || app.getApplicationUrl() == null || app.getApplicationUrl().isEmpty()) {
            if (app != null) {
                log.warn("Application {} has no application URL configured", app.getName());
            }
            return false;
        }

        String url = app.getApplicationUrl();

        try {
            log.info("Checking application URL for {} at: {}", app.getName(), url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            boolean isLive = response.getStatusCode().is2xxSuccessful();
            log.info("Application URL status for {}: {}", app.getName(), isLive);
            return isLive;
        } catch (RestClientException e) {
            log.warn("Failed to check application URL for {}: {}", app.getName(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking application URL for {}", app.getName(), e);
            return false;
        }
    }

    private boolean checkApiUrlLiveStatus(ApplicationConfiguration app) {
        if (app.getApiHealthEndPoint() == null || app.getApiHealthEndPoint().isEmpty()) {
            log.warn("Application {} has no API URL configured", app.getName());
            return false;
        }

        try {
            log.info("Checking API URL for {} at: {}", app.getName(), app.getApiHealthEndPoint());
            ResponseEntity<String> response = restTemplate.getForEntity(app.getApiHealthEndPoint(), String.class);
            boolean isLive = response.getStatusCode().value() == 200;
            log.info("API URL status for {}: {}", app.getName(), isLive);
            return isLive;
        } catch (RestClientException e) {
            log.warn("Failed to check API URL for {}: {}", app.getName(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking API URL for {}", app.getName(), e);
            return false;
        }
    }

    /**
     * Generic method to execute deployment actions
     */
    private DeploymentResponse executeDeploymentAction(
            String applicationName,
            String endpoint,
            HttpMethod method,
            String action) {
        try {
            String url = deployerProperties.getBaseUrl() + endpoint;
            HttpEntity<String> entity = createRequestEntity();

            log.info("Executing {} action for {} at: {}", action, applicationName, url);
            ResponseEntity<Object> response = restTemplate.exchange(
                    url, method, entity, Object.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return DeploymentResponse.builder()
                        .applicationName(applicationName)
                        .action(action)
                        .success(true)
                        .message("Action completed successfully")
                        .data(response.getBody())
                        .build();
            } else {
                return DeploymentResponse.builder()
                        .applicationName(applicationName)
                        .action(action)
                        .success(false)
                        .message("Action failed with status: " + response.getStatusCode())
                        .build();
            }
        } catch (RestClientException e) {
            log.error("Error executing {} for {}: {}", action, applicationName, e.getMessage(), e);
            return DeploymentResponse.builder()
                    .applicationName(applicationName)
                    .action(action)
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Create HTTP headers with API key
     */
    private HttpEntity<String> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (deployerProperties.getApiKey() != null && !deployerProperties.getApiKey().isEmpty()) {
            headers.set("X-API-Key", deployerProperties.getApiKey());
        }
        return new HttpEntity<>(headers);
    }
}
