# Admin Hub Application

A comprehensive web-based administration dashboard for managing applications, deployments, and server infrastructure. Built with Spring Boot 3.5.9 backend and Angular 21 frontend.

## 📋 Overview

Admin Hub is a unified management platform that provides:
- **Application Deployment Management**: Checkout, build, verify, and deploy applications with a single click
- **Real-time Monitoring**: Monitor application status, server health, and running services
- **Log Monitoring**: View application logs in real-time
- **Service Management**: Start, stop, and restart services
- **Health Checks**: Continuous health monitoring of deployed services
- **User Authentication**: Secure login with JWT-based authentication

## 🏗️ Architecture

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.5.9
- **Language**: Java 17
- **Build Tool**: Maven

#### Frontend
- **Framework**: Angular 21
- **Language**: TypeScript 5.9
- **Build Tool**: Angular CLI
- **Package Manager**: npm

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Node.js 18+** and npm 9+
- **Maven 3.8+**
- Git

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd admin-hub
   ```

2. **Configure environment variables**
   ```bash
   # Edit src/main/resources/application.properties
   # Required configurations:
   - jwt.secret=<your-jwt-secret>
   - admin.email=<admin-username>
   - admin.password=<admin-password>
   - admin.name=<admin-full-name>
   - deployer.base-url=<deployer-api-url>
   - deployer.api-key=<deployer-api-key>
   ```

3. **Build the project**
   ```bash
   mvn clean package
   ```

4. **Run the application**
   ```bash
   # Development mode with Spring Boot
   mvn spring-boot:run
   
   # Or run the JAR file
   java -jar target/admin-hub-1.0.0.jar
   ```

   The backend API will be available at: `http://localhost:8089`

### Frontend Setup

1. **Navigate to UI directory**
   ```bash
   cd ui
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure API endpoint**
   ```bash
   # Edit src/environments/environment.ts for development
   # Edit src/environments/environment.development.ts for dev server
   # Update the apiUrl to match your backend URL
   ```

4. **Development server**
   ```bash
   npm start
   # Application will be available at http://localhost:4200
   ```

5. **Build for production**
   ```bash
   npm run build
   # Output will be in dist/ directory
   ```

## 📖 API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/validate` - Validate token

### Deployment
- `GET /api/deployment/applications` - List all applications
- `GET /api/deployment/applications/{name}/status` - Get application status
- `GET /api/deployment/applications/{name}/logs` - Fetch application logs
- `POST /api/deployment/applications/{name}/checkout` - Checkout latest code
- `POST /api/deployment/applications/{name}/build` - Build application
- `POST /api/deployment/applications/{name}/verify` - Verify deployment
- `POST /api/deployment/applications/{name}/deploy` - Deploy application
- `POST /api/deployment/applications/{name}/restart` - Restart service
- `POST /api/deployment/applications/{name}/stop` - Stop service
- `POST /api/deployment/health` - Check deployer health

### Server
- `GET /api/server/health` - Server health summary
- `GET /api/server/services` - List running services

### Health & Monitoring
- `GET /actuator/health` - Application health check
- `GET /actuator/metrics` - Application metrics

## 🔐 Security

### Authentication
- JWT-based authentication with configurable expiration (default: 7 days)
- Secure password storage using Spring Security
- CORS configuration for frontend access

### Authorization
- Role-based access control (RBAC)
- Protected endpoints requiring valid JWT token
- HTTP interceptors for automatic token attachment

## 🔧 Configuration

### Application Properties

#### Server Configuration
```properties
server.port=8089
spring.application.name=admin-hub
```

#### JWT Configuration
```properties
jwt.secret=<your-secret-key>
jwt.expiration=604800000  # 7 days in milliseconds
```

#### Admin Credentials
```properties
admin.email=administrator
admin.password=<secure-password>
admin.name=Admin Hub Administrator
```

#### Deployer Integration
```properties
deployer.base-url=http://localhost:8000
deployer.api-key=<api-key-for-deployer>
```

### Environment Profiles

- **Development** (`application.properties`): Local development configuration
- **Production** (`application-prod.properties`): Production deployment configuration

To run with production profile:
```bash
java -jar target/admin-hub-1.4.0.jar --spring.profiles.active=prod
```

## 📊 Key Features

### Dashboard Component
The main dashboard (`dashboard.component.ts`) provides:

- **Application Management**
  - List all configured applications
  - Search and filter applications
  - Real-time application status monitoring
  - Live application status indicators

- **Deployment Actions**
  - Status: Check application deployment status
  - Logs: View last 100 log entries
  - Checkout: Pull latest code from repository
  - Build: Compile and build the application
  - Verify: Run verification tests
  - Deploy: Full deployment pipeline
  - Stop: Gracefully stop the service
  - Restart: Restart the service

- **Server Monitoring**
  - Real-time server health status
  - Running services list
  - Resource utilization metrics
  - Health check intervals (30-second refresh)

- **Health Checks**
  - Continuous deployer health monitoring
  - Automatic application refresh on service recovery
  - Live status polling (10-second intervals)

### Services

#### DeploymentService
- Manages application configurations
- Executes deployment actions
- Fetches application logs and status
- Communicates with external deployer API

#### ServerService
- Fetches server health metrics
- Lists running services
- Monitors system resources

#### AuthService
- Handles user authentication
- Manages JWT tokens
- Token refresh and validation

## 🧪 Testing

### Backend Tests
```bash
mvn test
```

### Frontend Tests
```bash
cd ui
npm run test
```

## 📝 Logging

Logging configuration is defined in `src/main/resources/logback-spring.xml`:
- Console output for development
- File output for production (logs/ directory)
- Configurable log levels per package
- Rolling file appenders with daily rotation

View logs:
```bash
# Development logs
tail -f logs/admin-hub-app.log

# Watch recent logs
tail -100f logs/admin-hub-app.log
```

## 🚀 Production Deployment

### Systemd Service
Use the provided service file for production deployment:

```bash
sudo cp prod-deployment-scripts/adminhub-app.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable adminhub-app
sudo systemctl start adminhub-app
```

### Nginx Reverse Proxy
Configure Nginx using provided configuration:

```bash
sudo cp prod-deployment-scripts/adminhub.upvaly.com /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/adminhub.upvaly.com /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

