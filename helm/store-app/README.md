# Store Application Helm Chart

This Helm chart deploys the complete Online Store microservices application.

## Prerequisites

- Kubernetes 1.16+
- Helm 3.0+
- Docker for building images

## Installing the Chart

To install the chart with the release name `store-app`:

```bash
helm install store-app ./helm/store-app
```

## Using the deploy-with-helm.sh Script

For convenience, a script has been provided to build Docker images and deploy the application with Helm:

```bash
./deploy-with-helm.sh
```

The script will:
1. Build Docker images for all services (optional)
2. Deploy the application using Helm

## Configuration

The following table lists the configurable parameters of the chart and their default values:

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.imageRegistry` | Global Docker image registry | `docker.io/library` |
| `global.imagePullPolicy` | Global default image pull policy | `Never` |
| `beverageService.replicas` | Number of beverage service replicas | `2` |
| `managementService.replicas` | Number of management service replicas | `2` |
| `uiModule.replicas` | Number of UI module replicas | `1` |
| `dbHandler.replicas` | Number of DB handler replicas | `1` |

## Customizing the Deployment

You can override values from the command line:

```bash
helm install store-app ./helm/store-app --set beverageService.replicas=3
```

Or, provide a values file:

```bash
helm install store-app ./helm/store-app -f my-values.yaml
```

## Updating the Application

To update the application after making changes:

```bash
helm upgrade store-app ./helm/store-app
```

## Uninstalling the Chart

To uninstall/delete the `store-app` deployment:

```bash
helm uninstall store-app
```
