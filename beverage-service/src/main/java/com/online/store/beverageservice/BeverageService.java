package com.online.store.beverageservice;

import jakarta.ws.rs.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/beverages")
public class BeverageService {

    private static final Logger logger = LoggerFactory.getLogger(BeverageService.class);
    private static final String DB_HANDLER_URL = "http://db:9999/v1/beverages";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllBeverages() {
        Client client = ClientBuilder.newClient();
        try (client) {
            WebTarget target = client.target(DB_HANDLER_URL);
            Response response = target.request(MediaType.APPLICATION_JSON).get();
            if (response.getStatus() == 200) {
                logger.info("Successfully fetched beverages from DB-Handler");
                return Response.ok(response.readEntity(String.class)).build();
            } else {
                String errorMsg = response.readEntity(String.class);
                logger.error("Failed to fetch beverages. Status: {}. Error: {}", response.getStatus(), errorMsg);
                return Response.status(response.getStatus()).entity(errorMsg).build();
            }
        } catch (Exception e) {
            logger.error("Exception occurred while connecting to DB-Handler", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error connecting to DB-Handler").build();
        }
    }
}
