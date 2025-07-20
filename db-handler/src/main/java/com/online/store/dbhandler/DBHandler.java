package com.online.store.dbhandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/v1/beverages")
public class DBHandler {

    private static final Logger logger = LoggerFactory.getLogger(DBHandler.class);

    private static final String DATA_FILE = "beverages.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DBHandler() {
        createJsonFileIfNotExists();
        insertDummyData();
    }

    private void createJsonFileIfNotExists() {
        java.nio.file.Path path = Paths.get(DATA_FILE);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
                try (FileWriter writer = new FileWriter(DATA_FILE)) {
                    writer.write("[]");
                }
                logger.info("Created new beverages.json file");
            } catch (IOException e) {
                logger.error("Failed to create beverages.json file: {}", e.getMessage());
            }
        }
        else {
            logger.info("Beverages file already exists: {}", DATA_FILE);
        }
    }



    private synchronized List<Map<String, Object>> readBeveragesFromFile() throws IOException {
        java.nio.file.Path path = Paths.get(DATA_FILE);
        if (!Files.exists(path)) {
            logger.info("Beverages file does not exist, creating new file: {}", DATA_FILE);
            Files.createFile(path);
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write("[]"); // Initialize with an empty JSON array
            } catch (IOException e) {
                logger.error("Failed to create beverages file: {}", e.getMessage());
                throw e;
            }
        }
        try {
            return objectMapper.readValue(new File(DATA_FILE), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            logger.error("Error reading beverages file, attempting recovery: {}", e.getMessage());
            Files.write(path, "[]".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return new ArrayList<>();
        }
    }

    private synchronized void writeBeveragesToFile(List<Map<String, Object>> beverages) throws IOException {
        java.nio.file.Path path = Paths.get(DATA_FILE);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), beverages);
    }

    private void insertDummyData() {
        logger.info("Checking for dummy data in file: {}", DATA_FILE);
        java.nio.file.Path path = Paths.get(DATA_FILE);
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            if (beverages.isEmpty()) {
                List<Map<String, Object>> dummyBeverages = new ArrayList<>();
                Map<String, Object> cola = new HashMap<>();
                cola.put("id", 1);
                cola.put("name", "Cola Bottle");
                cola.put("volume", 0.5);
                cola.put("isAlcoholic", false);
                cola.put("volumePercent", 0.0);
                cola.put("price", 1.5);
                cola.put("supplier", "CocaCola");
                cola.put("inStock", 100);
                cola.put("type", "bottle");
                dummyBeverages.add(cola);

                Map<String, Object> beer = new HashMap<>();
                beer.put("id", 2);
                beer.put("name", "Beer Bottle");
                beer.put("volume", 0.33);
                beer.put("isAlcoholic", true);
                beer.put("volumePercent", 5.0);
                beer.put("price", 2.0);
                beer.put("supplier", "Brewery");
                beer.put("inStock", 50);
                beer.put("type", "bottle");
                dummyBeverages.add(beer);

                Map<String, Object> crate = new HashMap<>();
                crate.put("id", 3);
                crate.put("bottle", beer);
                crate.put("noOfBottles", 20);
                crate.put("price", 35.0);
                crate.put("inStock", 10);
                crate.put("type", "crate");
                dummyBeverages.add(crate);

                writeBeveragesToFile(dummyBeverages);
                logger.info("Inserted dummy beverages into JSON file");
            }
        } catch (IOException e) {
            logger.error("Error initializing dummy data", e);
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
            Map<String, Object> beverage = objectMapper.readValue(beverageJson, new TypeReference<Map<String, Object>>() {});
            // Assign a new id if not present
            if (!beverage.containsKey("id")) {
                int maxId = 0;
                for (Map<String, Object> obj : beverages) {
                    Object idObj = obj.get("id");
                    if (idObj instanceof Number) {
                        maxId = Math.max(maxId, ((Number) idObj).intValue());
                    } else if (idObj != null) {
                        try {
                            maxId = Math.max(maxId, Integer.parseInt(idObj.toString()));
                        } catch (NumberFormatException ignore) {}
                    }
                }
                beverage.put("id", maxId + 1);
            }
            beverages.add(beverage);
            writeBeveragesToFile(beverages);
            logger.info("Added new beverage: {}", beverageJson);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            logger.error("Error adding beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error adding beverage").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteBeverage(@PathParam("id") String id) {
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            List<Map<String, Object>> updated = new ArrayList<>();
            boolean found = false;
            for (Map<String, Object> obj : beverages) {
                Object idObj = obj.get("id");
                if (idObj != null && idObj.toString().equals(id)) {
                    found = true;
                } else {
                    updated.add(obj);
                }
            }
            if (found) {
                writeBeveragesToFile(updated);
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
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBeverage(@PathParam("id") String id, String beverageJson) {
        try {
            List<Map<String, Object>> beverages = readBeveragesFromFile();
            boolean found = false;
            Map<String, Object> updatedBeverage = objectMapper.readValue(beverageJson, new TypeReference<Map<String, Object>>() {});
            for (int i = 0; i < beverages.size(); i++) {
                Object idObj = beverages.get(i).get("id");
                if (idObj != null && idObj.toString().equals(id)) {
                    updatedBeverage.put("id", idObj); // Ensure the id stays the same
                    beverages.set(i, updatedBeverage);
                    found = true;
                    break;
                }
            }
            if (found) {
                writeBeveragesToFile(beverages);
                logger.info("Updated beverage with id: {}", id);
                return Response.ok().entity("Beverage updated successfully").build();
            } else {
                logger.warn("No beverage found with id: {}", id);
                return Response.status(Response.Status.NOT_FOUND).entity("Beverage not found").build();
            }
        } catch (Exception e) {
            logger.error("Error updating beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating beverage").build();
        }
    }
}
