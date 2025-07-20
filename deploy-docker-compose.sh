#!/bin/bash
set -e

PROJECT_NAME="soa_assign_2" # Change this to your project name or image prefix
export DB_HANDLER_URL=http://db:9999/v1/beverages

function build() {
  echo "Building Maven modules..."
  # Remove beverages.json before build to ensure a clean state
  if [ -f db-handler/beverages.json ]; then
    echo "Removing db-handler/beverages.json..."
    rm db-handler/beverages.json
  fi
  touch db-handler/beverages.json
  mvn clean package -DskipTests
}

function install() {
  echo "Stopping and removing old containers, images, volumes, and networks..."
  if command -v podman-compose &> /dev/null; then
    podman-compose down -v --remove-orphans
    podman image prune -af
  else
    docker-compose down -v --remove-orphans
    docker container prune -f
    docker volume prune -f
    docker network prune -f
    docker images | grep "$PROJECT_NAME" | awk '{print $3}' | xargs -r docker rmi -f || true
  fi

  build

  echo "Building and starting containers..."
  if command -v podman-compose &> /dev/null; then
    podman-compose build
    podman-compose up -d
  else
    docker-compose build
    docker-compose up -d
  fi

  echo "All services are up!"
  echo "UI: http://localhost"
  echo "Beverage Service API: http://localhost:8080"
  echo "Management Service API: http://localhost:8090"
  echo "DB Handler API: http://localhost:9999"
}

function uninstall() {
  echo "Stopping and removing all containers, images, volumes, and networks..."
  if command -v podman-compose &> /dev/null; then
    podman-compose down -v --remove-orphans
    podman image prune -af
  else
    docker-compose down -v --remove-orphans
    docker container prune -f
    docker volume prune -f
    docker network prune -f
    docker images | grep "$PROJECT_NAME" | awk '{print $3}' | xargs -r docker rmi -f || true
  fi
  echo "Uninstall complete."
}

case "$1" in
  build)
    build
    ;;
  install)
    install
    ;;
  uninstall)
    uninstall
    ;;
  *)
    echo "Usage: $0 {build|install|uninstall}"
    exit 1
    ;;
esac