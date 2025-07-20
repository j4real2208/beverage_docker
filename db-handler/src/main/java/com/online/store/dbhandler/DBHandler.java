package com.online.store.dbhandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.kubernetes.client.openapi.ApiException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.util.Config;
import jakarta.ws.rs.*;



@jakarta.ws.rs.Path("/v1/beverages")
public class DBHandler {

    private static final Logger logger = LoggerFactory.getLogger(DBHandler.class);

    private static final String CONFIG_MAP_PATH = "/etc/config/beverages.json";
    private static final String CONFIG_MAP_NAME = "beverages-config";
    private static final String NAMESPACE = "default";
    private static final String PERSISTENT_DATA_PATH = "/app/data";

    private CoreV1Api api;
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private static String getDataFilePath() {
        try {
            Path configPath = Paths.get(CONFIG_MAP_PATH);
            if (Files.exists(configPath)) {
                logger.info("Using beverages.json from ConfigMap at {}", CONFIG_MAP_PATH);
                return CONFIG_MAP_PATH;
            } else {
                logger.warn("ConfigMap file not found at {}. Falling back to default path.", CONFIG_MAP_PATH);
                return "beverages.json";
            }
        } catch (Exception e) {
            logger.error("Error accessing ConfigMap file: {}", e.getMessage());
            return "beverages.json";
        }
    }

    private static final String DATA_FILE = getDataFilePath();

    public DBHandler() {
        System.out.println("DBHandler constructor called");
        try {
            // Explicitly use in-cluster config which works inside Kubernetes
            logger.info("Initializing Kubernetes client with in-cluster configuration");
            ApiClient client = io.kubernetes.client.util.ClientBuilder.cluster().build();
            Configuration.setDefaultApiClient(client);
            api = new CoreV1Api();
            // Check if the API client is initialized with listing config maps
            logger.info("the config map is there {}", !Objects.requireNonNull(api.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE, null)
                    .getData()).get("beverages.json").isEmpty());

            logger.info("Kubernetes client initialized successfully");
        } catch (IOException e) {
            logger.warn("Failed to initialize Kubernetes client: {}. Some functionality may be limited.", e.getMessage());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized List<Map<String, Object>> readBeveragesFromFile() throws IOException {
        Path configPath = Paths.get(CONFIG_MAP_PATH);
        if (!Files.exists(configPath)) {
            logger.error("ConfigMap file not found at {}. Cannot read beverages.", CONFIG_MAP_PATH);
            return List.of();
        }
        return objectMapper.readValue(Files.readString(configPath), new TypeReference<>() {
        });
    }

    private synchronized void updateConfigMap(List<Map<String, Object>> beverages) {
        try {
            logger.info("Attempting to update ConfigMap: {} in namespace: {}", CONFIG_MAP_NAME, NAMESPACE);

            // Check if API client is properly initialized
            if (api == null) {
                logger.error("Kubernetes API client is null. Cannot update ConfigMap.");
                return;
            }

            // Read the current ConfigMap
            logger.info("Reading existing ConfigMap...");
            V1ConfigMap configMap = api.readNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE, null);
            logger.info("Successfully read ConfigMap: {}", configMap.getMetadata().getName());

            // Convert beverages to JSON
            String updatedData = objectMapper.writeValueAsString(beverages);
            logger.info("Prepared updated data for ConfigMap with {} beverages", beverages.size());

            // Check if getData() is null
            if (configMap.getData() == null) {
                logger.error("ConfigMap data section is null, creating new data map");
                configMap.setData(new java.util.HashMap<>());
            }

            // Update the data
            configMap.getData().put("beverages.json", updatedData);

            // Replace the ConfigMap
            logger.info("Sending ConfigMap update request...");
            api.replaceNamespacedConfigMap(CONFIG_MAP_NAME, NAMESPACE, configMap, null, null, null, null);
            logger.info("ConfigMap updated successfully!");
        } catch (Exception e) {
            logger.error("Failed to update ConfigMap: {} - {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
        }
    }

    private synchronized void writePersistentData(String fileName, String content) {
        try {
            Path filePath = Paths.get(PERSISTENT_DATA_PATH, fileName);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content);
            logger.info("Data written to persistent storage at {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to write data to persistent storage: {}", e.getMessage());
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBeverages() {
        try {
            logger.info("Get beverages from file: {}", DATA_FILE);
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            logger.info("Fetched {} beverages from JSON file", beverages.size());
            return Response.ok(objectMapper.writeValueAsString(beverages)).build();
        } catch (Exception e) {
            logger.error("Error fetching beverages from file", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error fetching beverages").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addBeverage(String beverageJson) {
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            Map<String, Object> beverage = objectMapper.readValue(beverageJson, new TypeReference<>() {
            });
            if (!beverage.containsKey("id")) {
                int maxId = beverages.stream()
                    .map(obj -> obj.get("id"))
                    .filter(id -> id instanceof Number)
                    .mapToInt(id -> ((Number) id).intValue())
                    .max()
                    .orElse(0);
                beverage.put("id", maxId + 1);
            }
            beverages.add(beverage);
            updateConfigMap(beverages);
            logger.info("Added new beverage: {}", beverageJson);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            logger.error("Error adding beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error adding beverage").build();
        }
    }

    @DELETE
    @jakarta.ws.rs.Path("/{id}")
    public Response deleteBeverage(@PathParam("id") String id) {
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            boolean found = beverages.removeIf(obj -> id.equals(obj.get("id")));
            if (found) {
                updateConfigMap(beverages);
                logger.info("Deleted beverage with id: {}", id);
                return Response.noContent().build();
            } else {
                logger.warn("No beverage found with id: {}", id);
                return Response.status(Response.Status.NOT_FOUND).entity("Beverage not found").build();
            }
        } catch (Exception e) {
            logger.error("Error deleting beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting beverage").build();
        }
    }

    @PUT
    @jakarta.ws.rs.Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBeverage(@PathParam("id") String id, String beverageJson) {
        logger.trace("Updating beverage: {}", beverageJson);
        if (id == null || id.isEmpty()) {
            logger.error("ID is null or empty during update");
            return Response.status(Response.Status.BAD_REQUEST).entity("ID cannot be null or empty").build();
        }
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            if (beverages == null || beverages.isEmpty()) {
                logger.warn("No beverages found to update");
                return Response.status(Response.Status.NOT_FOUND).entity("No beverages found").build();
            }
            logger.info("Current beverages: {}", beverages);
            boolean found = false;
            Map<String, Object> updatedBeverage = objectMapper.readValue(beverageJson, new TypeReference<>() {});

            // print the updated beverage for debugging
            logger.info("Updating beverage with id: {} with data: {}", id, updatedBeverage);

            for (int i = 0; i < beverages.size(); i++) {
                // Get the id from the beverage and handle different types (Integer vs String)
                Object beverageId = beverages.get(i).get("id");

                // Convert both to strings for comparison
                String beverageIdStr = beverageId != null ? beverageId.toString() : null;

                if (id.equals(beverageIdStr)) {
                    logger.info("Found matching beverage at index: {}", i);
                    updatedBeverage.put("id", beverageId); // Keep the original ID object (maintain type)
                    beverages.set(i, updatedBeverage);
                    found = true;
                    break;
                }
            }

            if (found) {
                logger.info("Beverage found, updating ConfigMap with {} beverages", beverages.size());
                updateConfigMap(beverages);
                logger.info("Updated beverage with id: {}", id);
                return Response.ok().entity("Beverage updated successfully").build();
            } else {
                logger.warn("No beverage found with id: {} during update", id);
                return Response.status(Response.Status.NOT_FOUND).entity("Beverage not found").build();
            }
        } catch (Exception e) {
            logger.error("Error updating beverage: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating beverage").build();
        }
    }

    @PUT
    @jakarta.ws.rs.Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBeverages(List<Map<String, Object>> beverages) {
        try {
            updateConfigMap(beverages);
            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error updating beverages: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveData(Map<String, Object> data) {
        try {
            String fileName = "data.json";
            String content = objectMapper.writeValueAsString(data);
            writePersistentData(fileName, content);
            return Response.ok().entity("Data saved successfully").build();
        } catch (Exception e) {
            logger.error("Error saving data: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error saving data").build();
        }
    }
}
