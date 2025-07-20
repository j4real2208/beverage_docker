package com.online.store.managementservice;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;

public class ManagementServiceMain {
    public static final String BASE_URI = "http://0.0.0.0:8090/";

    public static HttpServer startServer() {
        final ResourceConfig rc = new ResourceConfig()
                .register(PingResource.class)
                .register(ManagementService.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        System.out.println("Management Service started at " + BASE_URI + " (Press Ctrl+C to stop)");
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
