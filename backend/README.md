# Backend - Contact Management System

Spring Boot microservices architecture for the Contact Management System.

## Services

### 1. Eureka Server
Service discovery and registration server. All microservices register with Eureka.

**Port**: 8761

### 2. API Gateway
Centralized entry point for all client requests. Routes requests to appropriate microservices.

**Port**: 8080

### 3. Contact Service
Core microservice handling contact management operations (CRUD).

**Port**: 8081

### 4. Common Module
Shared utilities, DTOs, exceptions, and common configurations used across all microservices.

## Technology Stack

- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Cloud Netflix Eureka (Service Discovery)
- Spring Cloud Gateway (API Gateway)
- Spring Data JPA
- PostgreSQL
- Maven

## Building the Project

```bash
# Build all modules
mvn clean install

# Build specific module
cd contact-service
mvn clean install
```

## Running Services

### Prerequisites
- PostgreSQL database running
- Java 17+

### Start Services (in order)

1. **Eureka Server**
```bash
cd eureka-server
mvn spring-boot:run
```

2. **Contact Service**
```bash
cd contact-service
mvn spring-boot:run
```

3. **API Gateway**
```bash
cd api-gateway
mvn spring-boot:run
```

## Database Configuration

Update `application.properties` or `application.yml` in each service with your PostgreSQL connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/contactdb
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## API Endpoints

Once services are running:
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- Contact Service (direct): http://localhost:8081
