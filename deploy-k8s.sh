#!/bin/bash
set -e

PROJECT_NAME="soa_assign_2" # Change this to your project name or image prefix
export DB_HANDLER_URL=http://db:9999/v1/beverages

function build_k8s_images() {
  echo "Removing old Docker images..."
  docker rmi beverage-service:latest db-handler:latest management-service:latest ui-module:latest || echo "No images to remove."

  echo "Building JAR files for services..."
  mvn clean package -DskipTests

  echo "Building Docker images for Kubernetes..."
  docker build -t beverage-service:latest ./beverage-service
  docker build -t db-handler:latest ./db-handler
  docker build -t management-service:latest ./management-service
  docker build -t ui-module:latest ./ui-module

  echo "Docker images built successfully."
}

function ensure_minikube_running() {
  echo "Ensuring Minikube cluster is running..."
  if ! minikube status | grep -q "Running"; then
    echo "Minikube is not running. Starting Minikube..."
    minikube start || {
      echo "Failed to start Minikube. Please ensure Minikube is installed and configured."
      exit 1
    }
  else
    echo "Minikube is already running."
  fi
}

function delete_existing_images() {
  echo "Deleting existing Docker images in Minikube..."
  minikube image rm beverage-service:latest db-handler:latest management-service:latest ui-module:latest || echo "No images to remove."
}

function load_images_into_minikube() {
  echo "Loading images into Minikube..."
  minikube image load beverage-service:latest
  minikube image load db-handler:latest
  minikube image load management-service:latest
  minikube image load ui-module:latest
}

function deploy_k8s() {
  ensure_minikube_running
  delete_existing_images
  build_k8s_images
  load_images_into_minikube

  echo "Deploying Kubernetes resources..."
  kubectl apply -f k8s/configmap.yaml
  kubectl apply -f k8s/statefulset.yaml
  kubectl apply -f k8s/deployment.yaml
  kubectl apply -f k8s/services.yaml
  kubectl apply -f k8s/rbac.yaml
}

function uninstall_k8s() {
  echo "Stopping and removing all Kubernetes resources..."
  kubectl delete -f k8s/configmap.yaml
  kubectl delete -f k8s/statefulset.yaml
  kubectl delete -f k8s/deployment.yaml
  kubectl delete -f k8s/services.yaml
  kubectl delete -f k8s/rbac.yaml

  echo "Stopping Minikube..."
#  minikube stop
}

case "$1" in
  build)
    build_k8s_images
    ;;
  deploy)
    deploy_k8s
    ;;
  uninstall-k8s)
    uninstall_k8s
    ;;
  *)
    echo "Usage: $0 {build|deploy|uninstall-k8s}"
    exit 1
    ;;
esac