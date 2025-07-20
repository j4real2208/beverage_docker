package com.online.store.managementservice;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

@Path("/ping")
public class PingResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response ping() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "pong");
        return Response.ok(response).build();
    }
}
