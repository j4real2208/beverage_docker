package com.online.store.managementservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/management/beverages")
public class ManagementService {

    private static final Logger logger = LoggerFactory.getLogger(ManagementService.class);
    private static final String DB_HANDLER_URL = System.getenv("DB_HANDLER_URL");

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBeverages() {
        Client client = ClientBuilder.newClient();
        try (client) {
            WebTarget target = client.target(DB_HANDLER_URL);
            Response response = target.request(MediaType.APPLICATION_JSON).get();
            logger.info("Fetched beverages from DB-Handler, status: {}", response.getStatus());
            return Response.status(response.getStatus()).entity(response.readEntity(String.class)).build();
        } catch (Exception e) {
            logger.error("Error fetching beverages from DB-Handler", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error fetching beverages").build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addBeverage(String beverage) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(beverage);
            // Basic type check
            if (node.has("bottle")) {
                // Crate validation
                if (!node.has("noOfBottles") || !node.has("price") || !node.has("inStock")) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Missing required fields for Crate").build();
                }
                if (node.get("noOfBottles").asInt() <= 0 || node.get("price").asDouble() < 0 || node.get("inStock").asInt() < 0) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Invalid values for Crate").build();
                }
            } else {
                // Bottle validation
                String[] required = {"name", "volume", "isAlcoholic", "volumePercent", "price", "supplier", "inStock"};
                for (String field : required) {
                    if (!node.has(field)) {
                        return Response.status(Response.Status.BAD_REQUEST).entity("Missing required field: " + field).build();
                    }
                }
                if (node.get("volume").asDouble() <= 0 || node.get("price").asDouble() < 0 || node.get("inStock").asInt() < 0 || node.get("volumePercent").asDouble() < 0) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Invalid values for Bottle").build();
                }
            }
        } catch (Exception e) {
            logger.error("Invalid beverage JSON", e);
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid beverage JSON").build();
        }
        Client client = ClientBuilder.newClient();
        try (client) {
            WebTarget target = client.target(DB_HANDLER_URL);
            try (Response response = target.request().post(Entity.json(beverage))) {
                logger.info("Added beverage, status: {}", response.getStatus());
                if (response.getStatus() >= 200 && response.getStatus() < 300) {
                    return Response
                            .ok("[{\"status\": \"successfully created!\"}]")
                            .build();
                } else {
                    return Response.status(response.getStatus()).entity(response.readEntity(String.class)).build();
                }
            }
        } catch (Exception e) {
            logger.error("Error adding beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error adding beverage").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteBeverage(@PathParam("id") String id) {
        Client client = ClientBuilder.newClient();
        try (client) {
            WebTarget target = client.target(DB_HANDLER_URL + "/" + id);
            try (Response response = target.request().delete()) {
                logger.info("Deleted beverage with id: {}, status: {}", id, response.getStatus());
                return Response.status(response.getStatus()).entity(response.readEntity(String.class)).build();
            }
        } catch (Exception e) {
            logger.error("Error deleting beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting beverage").build();
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBeverage(@PathParam("id") String id, String beverage) {
        Client client = ClientBuilder.newClient();
        try (client) {
            WebTarget target = client.target(DB_HANDLER_URL + "/" + id);
            try (Response response = target.request().put(Entity.json(beverage))) {
                logger.info("Updated beverage with id: {}, status: {}", id, response.getStatus());
                return Response.status(response.getStatus()).entity(response.readEntity(String.class)).build();
            }
        } catch (Exception e) {
            logger.error("Error updating beverage", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating beverage").build();
        }
    }
}
