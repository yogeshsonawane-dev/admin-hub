import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment.development';
import { AuthService } from './auth.service';

export interface ApplicationConfig {
  name: string;
  git_url?: string;
  branch?: string;
  build_type?: string;
  artifact_path?: string;
  service_name?: string;
  deploy_path?: string;
  application_url?: string;
  api_health_end_point?: string;
  symlink?: string;
}

export interface ApplicationHealthStatus {
  applicationName: string;
  applicationUrlLive: boolean;
  apiUrlLive: boolean;
  healthy: boolean;
  message?: string;
}

export interface DeploymentResponse {
  applicationName: string;
  action: string;
  success: boolean;
  message: string;
  data?: any;
  status?: string;
  logs?: string;
  details?: any;
}

@Injectable({
  providedIn: 'root'
})
export class DeploymentService {
  private apiUrl = environment.apiUrl || 'http://localhost:8089/api';
  private deploymentBaseUrl = `${this.apiUrl}/deployment`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  /**
   * Check health of deployment service
   */
  getHealth(): Observable<any> {
    return this.http.get(`${this.deploymentBaseUrl}/health`);
  }

  /**
   * Get all configured applications
   */
  getApplications(): Observable<ApplicationConfig[]> {
    return this.http.get<ApplicationConfig[]>(`${this.deploymentBaseUrl}/applications`);
  }

  /**
   * Checkout application repository
   */
  checkout(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/checkout/${applicationName}`,
      {}
    );
  }

  /**
   * Build application
   */
  build(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/build/${applicationName}`,
      {}
    );
  }

  /**
   * Verify build artifact
   */
  verify(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/verify/${applicationName}`,
      {}
    );
  }

  /**
   * Deploy application
   */
  deploy(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/deploy/${applicationName}`,
      {}
    );
  }

  /**
   * Restart application service
   */
  restart(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/restart/${applicationName}`,
      {}
    );
  }

  /**
   * Stop application service
   */
  stop(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/stop/${applicationName}`,
      {}
    );
  }

  /**
   * Get application status
   */
  getStatus(applicationName: string): Observable<DeploymentResponse> {
    return this.http.get<DeploymentResponse>(
      `${this.deploymentBaseUrl}/status/${applicationName}`
    );
  }

  /**
   * Get application logs
   */
  getLogs(applicationName: string, lines: number = 1000): Observable<DeploymentResponse> {
    let params = new HttpParams();
    params = params.set('lines', lines.toString());
    return this.http.get<DeploymentResponse>(
      `${this.deploymentBaseUrl}/logs/${applicationName}`,
      { params }
    );
  }

  /**
   * Execute full deployment workflow
   */
  fullDeploy(applicationName: string): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/full-deploy/${applicationName}`,
      {}
    );
  }

  /**
   * Execute custom action
   */
  executeAction(applicationName: string, action: string, lines?: number): Observable<DeploymentResponse> {
    return this.http.post<DeploymentResponse>(
      `${this.deploymentBaseUrl}/execute`,
      { applicationName, action, lines }
    );
  }

  /**
   * Check if application is live by testing the application URL
   */
  checkAppLiveStatus(appName: string): Observable<ApplicationHealthStatus> {
    return this.http.get<ApplicationHealthStatus>(`${this.deploymentBaseUrl}/applications/${appName}/health`);
  }

  /**
   * Subscribe to health and app status updates via SSE
   */
  subscribeToHealthAndAppStatus(): Observable<any> {
    return new Observable(observer => {
      const token = this.authService.token;
      const url = `${this.deploymentBaseUrl}/health/stream`;
      let abortController: AbortController | null = null;

      if (!token) {
        observer.error(new Error('No authentication token available'));
        return () => {};
      }

      // Use AbortController for cleaner cancellation
      abortController = new AbortController();

      fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        signal: abortController.signal
      })
        .then(response => {
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          return response.body;
        })
        .then(body => {
          if (!body) {
            throw new Error('No response body');
          }

          const reader = body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          const processStream = async () => {
            try {
              while (true) {
                const { done, value } = await reader.read();

                if (done) {
                  if (buffer.trim()) {
                    this.parseSSEEvent(buffer, observer);
                  }
                  observer.complete();
                  break;
                }

                buffer += decoder.decode(value, { stream: true });
                const parts = buffer.split('\n\n');

                // Process complete events
                for (let i = 0; i < parts.length - 1; i++) {
                  if (parts[i].trim()) {
                    this.parseSSEEvent(parts[i], observer);
                  }
                }

                // Keep incomplete event in buffer
                buffer = parts[parts.length - 1];
              }
            } catch (error: any) {
              if (error.name !== 'AbortError') {
                observer.error(error);
              }
            }
          };

          processStream();
        })
        .catch((error: any) => {
          if (error.name !== 'AbortError') {
            observer.error(error);
          }
        });

      return () => {
        if (abortController) {
          abortController.abort();
        }
      };
    });
  }

  /**
   * Parse Server-Sent Events format
   */
  private parseSSEEvent(eventString: string, observer: any): void {
    const lines = eventString.split('\n');
    const event: { [key: string]: string } = {};

    for (const line of lines) {
      if (line.startsWith('event:')) {
        event['event'] = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        event['data'] = line.substring(5).trim();
      } else if (line.startsWith('id:')) {
        event['id'] = line.substring(3).trim();
      }
    }

    if (event['data'] && event['event']) {
      try {
        observer.next({
          type: event['event'],
          data: JSON.parse(event['data'])
        });
      } catch (error) {
        console.error('Error parsing SSE event data:', error);
      }
    }
  }
}
