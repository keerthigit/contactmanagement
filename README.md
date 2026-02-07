# Contact Management System

A full-stack Contact Management System built with React frontend and Spring Boot microservices backend.

## Architecture

- **Frontend**: React with modern hooks and context API
- **Backend**: Spring Boot microservices architecture
  - Contact Service: Core contact management operations
  - API Gateway: Centralized routing and load balancing
  - Eureka Server: Service discovery and registration
  - Common: Shared utilities and DTOs
- **Database**: PostgreSQL

## Project Structure

```
contactmanagement/
├── frontend/          # React application
├── backend/           # Spring Boot microservices
│   ├── contact-service/    # Contact management microservice
│   ├── api-gateway/        # API Gateway service
│   ├── eureka-server/      # Service discovery server
│   └── common/             # Shared modules
└── docker-compose.yml      # Docker orchestration
```

## Getting Started

### Prerequisites
- Node.js (v16+)
- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Docker (optional)

### Backend Setup
```bash
cd backend
mvn clean install
```

### Frontend Setup
```bash
cd frontend
npm install
npm start
```

## Development

See individual README files in `frontend/` and `backend/` directories for detailed setup instructions.
