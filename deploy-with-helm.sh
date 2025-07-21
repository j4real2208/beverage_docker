#!/bin/bash
set -e

# Generate timestamp for image tags
TIMESTAMP=$(date +%Y%m%d%H%M%S)
IMAGE_TAG="v-${TIMESTAMP}"

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

  # Set Docker to use Minikube's Docker daemon
  echo "Configuring Docker environment for Minikube..."
  eval $(minikube docker-env)
  echo "Docker environment configured for Minikube."
}

function clean_docker_images() {
  echo "Cleaning up old Docker images..."

  # Define the list of image names to clean
  local image_names=("beverage-service" "db-handler" "management-service" "ui-module")

  # Remove images from local Docker
  echo "Removing old images from local Docker..."
  for img in "${image_names[@]}"; do
    if [[ $(docker images -q ${img} 2> /dev/null) ]]; then
      docker rmi $(docker images ${img} -q) || echo "No ${img} images to remove locally"
    fi
  done

  # Remove images from Minikube's Docker (if different from local)
  echo "Removing old images from Minikube Docker..."
  ensure_minikube_running
  for img in "${image_names[@]}"; do
    if [[ $(docker images -q ${img} 2> /dev/null) ]]; then
      docker rmi -f $(docker images ${img} -q) || echo "No ${img} images to remove in Minikube"
    fi
  done

  echo "Docker image cleanup completed."
}

function build_images() {
  # First ensure minikube is running and Docker is configured
  ensure_minikube_running

  # Clean up old images first
  clean_docker_images

  echo "Building JAR files for services..."
  mvn clean package -DskipTests

  echo "Building Docker images with tag: ${IMAGE_TAG}..."
  docker build -t beverage-service:${IMAGE_TAG} ./beverage-service
  docker build -t db-handler:${IMAGE_TAG} ./db-handler
  docker build -t management-service:${IMAGE_TAG} ./management-service
  docker build -t ui-module:${IMAGE_TAG} ./ui-module

  echo "Docker images built successfully with tag: ${IMAGE_TAG}"
}

function deploy_with_helm() {
  # First ensure minikube is running
  ensure_minikube_running

  echo "Deploying application with Helm..."

  # Check if chart is already installed
  if helm list | grep -q "store-app"; then
    echo "Upgrading existing Helm deployment..."
    helm upgrade store-app ./helm/store-app --set global.imageTag=${IMAGE_TAG}
  else
    echo "Installing new Helm deployment..."
    helm install store-app ./helm/store-app --set global.imageTag=${IMAGE_TAG}
  fi

  echo "Deployment complete!"
}

function uninstall_helm_release() {
  echo "Uninstalling Helm release..."

  # Check if chart is installed before attempting to uninstall
  if helm list | grep -q "store-app"; then
    echo "Uninstalling store-app Helm release..."
    helm uninstall store-app
    echo "Helm release uninstalled successfully."
  else
    echo "No store-app Helm release found. Nothing to uninstall."
  fi
}

# Display help message
function show_help() {
  echo "Usage: $0 [option]"
  echo "Options:"
  echo "  -h, --help       Show this help message"
  echo "  -b, --build      Build Docker images with timestamped tags"
  echo "  -d, --deploy     Deploy application with Helm"
  echo "  -c, --clean      Clean up old Docker images"
  echo "  -u, --uninstall  Uninstall Helm release"
  echo "  -a, --all        Build images and deploy application"
  echo "If no options are provided, script will run in interactive mode"
}

# Main execution
echo "Online Store Application - Helm Deployment"
echo "==========================================="
echo "Image tag for this build: ${IMAGE_TAG}"
echo

# Process command line arguments if provided
if [[ $# -gt 0 ]]; then
  case "$1" in
    -h|--help)
      show_help
      exit 0
      ;;
    -b|--build)
      build_images
      ;;
    -d|--deploy)
      deploy_with_helm
      ;;
    -c|--clean)
      clean_docker_images
      ;;
    -u|--uninstall)
      uninstall_helm_release
      ;;
    -a|--all)
      build_images
      deploy_with_helm
      ;;
    *)
      echo "Unknown option: $1"
      show_help
      exit 1
      ;;
  esac
else
  # Interactive mode

  # Ask about cleaning images
  read -p "Do you want to clean up old Docker images? (y/n): " clean_choice
  if [[ $clean_choice == "y" || $clean_choice == "Y" ]]; then
    clean_docker_images
  fi

  # Build Docker images
  read -p "Do you want to build Docker images? (y/n): " build_choice
  if [[ $build_choice == "y" || $build_choice == "Y" ]]; then
    build_images
  fi

  # Deploy with Helm
  read -p "Do you want to deploy the application with Helm? (y/n): " deploy_choice
  if [[ $deploy_choice == "y" || $deploy_choice == "Y" ]]; then
    deploy_with_helm
  fi

  # Offer to uninstall
  read -p "Do you want to uninstall the Helm release? (y/n): " uninstall_choice
  if [[ $uninstall_choice == "y" || $uninstall_choice == "Y" ]]; then
    uninstall_helm_release
  fi
fi

# Display access information for the UI
if kubectl get service ui-module &> /dev/null; then
  echo ""
  echo "Access Information:"
  echo "-----------------"

  # Get service URL using Minikube
  echo "To access the UI module, run:"
  echo "  minikube service ui-module"

  echo ""
  echo "Or use port forwarding:"
  echo "  kubectl port-forward service/ui-module 8082:8082"
  echo "  Then visit: http://localhost:8082"
fi

echo "Script completed successfully!"
