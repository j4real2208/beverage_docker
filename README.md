# Beverage Store Microservices Project

This project is a microservices-based beverage store system, featuring a Java backend, a static UI frontend, and full deployment support for Docker Compose. It demonstrates separation of concerns, RESTful APIs, internal-only services, and a simple management UI.

## Project Structure

- **beverage-service/**: Exposes beverage data for viewing (GET only, Java/JAX-RS, Jersey, SLF4J, Lombok).
- **management-service/**: Handles CRUD operations for beverages (Java/JAX-RS, Jersey, SLF4J, Lombok).
- **db-handler/**: Handles direct file-based beverage storage (Java/JAX-RS, Jersey, SLF4J, Lombok).
- **ui-module/**: Static frontend (HTML/CSS/JS) served by Nginx, with a single unified view for all features.
- **docker-compose.yml**: Docker Compose configuration for local development.
- **deploy-docker-compose.sh**: Script to build and run the stack with Docker Compose.

## Features

- **Microservices Architecture**: Each service is independently deployable and scalable.
- **Unified UI**: All features (view, add, edit, delete) are available to all users in a single view.
- **Separation of Concerns**: UI, business logic, and data access are separated into different services.
- **Internal-Only Services**: db-handler is only accessible within the network.
- **Nginx Reverse Proxy**: Serves static UI and proxies API requests to the correct backend services.
- **Dummy Data**: On startup, the database is seeded with sample bottles and crates for demo purposes.

## Endpoints

- **UI**: 
  - `/` (served by Nginx)
- **Beverage Service**: 
  - `/api/beverages` (GET) — for viewing beverages (bottles and crates)
- **Management Service**: 
  - `/management/beverages` (GET, POST)
  - `/management/beverages/{id}` (PUT, DELETE)
- **DB Handler**: 
  - `/v1/beverages` (internal, not exposed externally)

## Deployment

### Docker Compose

1. Build and start all services:
   ```sh
   bash deploy-docker-compose.sh
   ```
2. Access the UI at [http://localhost:8088](http://localhost:8088)

## Nginx Proxy Configuration
- `/api/` → beverage-service (for viewing beverages)
- `/management/` → management-service (for CRUD)
- All other requests serve static files from the UI module

## Internal Communication
- beverage-service and management-service communicate with db-handler for data persistence.

## Technologies Used
- Java 21 (JAX-RS style REST)
- Nginx (for static UI and reverse proxy)
- Docker & Docker Compose
- Lombok & SLF4J for code simplification and logging

## Notes
- All sensitive operations and data access are protected by internal-only services.
- The UI is for demonstration and does not implement secure authentication.
- For production, implement proper authentication, HTTPS, and secure secrets management.

---

For more details, see the README files in each module and the Helm chart.
