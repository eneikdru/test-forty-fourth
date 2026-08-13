package com.eneik.production;

import com.eneik.production.api.SearchHttpServer;
import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 8080;

        String dbUrl = System.getenv("DB_URL");
        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://postgres:5432/materials";
        }
        String dbUser = System.getenv("DB_USER");
        if (dbUser == null) {
            dbUser = "materials_user";
        }
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = "materials_password";
        }

        System.out.println("Starting Material Search Service...");
        System.out.println("Configured Port: " + port);
        System.out.println("Database URL: " + dbUrl);

        // Wait for database connection
        int maxRetries = 15;
        Connection conn = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (dbUrl.startsWith("jdbc:postgresql:")) {
                    Class.forName("org.postgresql.Driver");
                } else if (dbUrl.startsWith("jdbc:h2:")) {
                    Class.forName("org.h2.Driver");
                }
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                conn.close();
                System.out.println("Successfully connected to database.");
                break;
            } catch (ClassNotFoundException e) {
                System.err.println("JDBC Driver class not found: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            } catch (SQLException e) {
                System.out.println("Database connection failed, retrying in 2 seconds (" + (i + 1) + "/" + maxRetries + "): " + e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }

        // Run Flyway migration
        System.out.println("Running database migrations via Flyway...");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .load();
            flyway.migrate();
            System.out.println("Database migrations applied successfully.");
        } catch (Exception e) {
            System.err.println("Flyway migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Start HTTP Server
        try {
            final String finalDbUrl = dbUrl;
            final String finalDbUser = dbUser;
            final String finalDbPassword = dbPassword;

            SearchHttpServer server = new SearchHttpServer(port, () -> DriverManager.getConnection(finalDbUrl, finalDbUser, finalDbPassword));
            server.start();
            System.out.println("Search HTTP Server is running on port " + port);

            // Keep main thread alive
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Search HTTP Server...");
                server.stop(1);
            }));
        } catch (IOException e) {
            System.err.println("Failed to start HTTP Server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
