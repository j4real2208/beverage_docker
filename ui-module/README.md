# UI Module for Beverage Store

This module provides a simple static frontend for the beverage store, served via Nginx. It supports basic login to distinguish between customer and employee roles.

## Structure
- index.html: Main entry point
- app.js: Handles login and API calls
- style.css: Basic styling
- nginx.conf: Nginx configuration for static serving and reverse proxy

## Nginx Proxy Configuration
- `/api/` requests are proxied to the beverage-service (for viewing beverages)
- `/management/` requests are proxied to the management-service (for CRUD operations)
- All other requests serve static files from the UI module

## Accessing Services (Kubernetes)
- UI: NodePort 30080 (http://<minikube-ip>:30080)
- Beverage Service: NodePort 30081 (http://<minikube-ip>:30081)
- Management Service: NodePort 30082 (http://<minikube-ip>:30082)
- All other services (db-handler, mongo) are only accessible internally

## Accessing Services (Docker Compose)
- UI: http://localhost
- Beverage Service: http://localhost:8080
- Management Service: http://localhost:8090
- DB Handler: http://localhost:9999
- MongoDB: localhost:27017

## Login
- Demo users are hardcoded in app.js:
  - customer / customer
  - employee / employee
- This is for demonstration only and not secure for production.

## CRUD Operations
- Customers can view bottles and crates separately.
- Employees can perform CRUD operations on beverages via the management-service endpoints.

## Deployment
- For Docker Compose: run `bash deploy-docker-compose.sh`
- For Kubernetes/Minikube: run `bash k8s-setup/deploy-minikube.sh`
