package com.eneik.production;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchApiContractTest {

    @Test
    public void testSearchApiContractStructureAndElements() throws Exception {
        // Load the OpenAPI contract YAML from the classpath/resources or as stream relative to the project root
        InputStream apiStream = getClass().getClassLoader().getResourceAsStream("docs/contracts/search_api.yaml");
        if (apiStream == null) {
            // Fallback for different build environments or project structures
            apiStream = getClass().getResourceAsStream("/docs/contracts/search_api.yaml");
        }
        if (apiStream == null) {
            // Fallback for file system based load if classpath lookup needs help
            java.io.File file = new java.io.File("docs/contracts/search_api.yaml");
            if (file.exists()) {
                apiStream = new java.io.FileInputStream(file);
            }
        }

        assertNotNull(apiStream, "OpenAPI search_api.yaml contract must be present.");

        String specContent;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(apiStream, StandardCharsets.UTF_8))) {
            specContent = reader.lines().collect(Collectors.joining("\n"));
        }

        // Validate essential OpenAPI elements of the contract
        assertTrue(specContent.contains("openapi: 3.0.3"), "Contract must be OpenAPI 3.0.3");
        assertTrue(specContent.contains("/api/materials/search"), "Contract must define the /api/materials/search endpoint");
        assertTrue(specContent.contains("name: q"), "Contract must define the search query parameter 'q'");
        assertTrue(specContent.contains("name: category"), "Contract must define the 'category' filter parameter");
        assertTrue(specContent.contains("name: status"), "Contract must define the 'status' filter parameter");
        assertTrue(specContent.contains("name: sort"), "Contract must define the 'sort' parameter");
        assertTrue(specContent.contains("name: page"), "Contract must define the 'page' parameter");
        assertTrue(specContent.contains("name: limit"), "Contract must define the 'limit' parameter");

        // Validate Schema definition presence
        assertTrue(specContent.contains("MaterialDTO"), "Contract must define a MaterialDTO schema");
        assertTrue(specContent.contains("SearchResponse"), "Contract must define a SearchResponse schema");
        assertTrue(specContent.contains("ErrorResponse"), "Contract must define an ErrorResponse schema");

        // Validate the DTO properties are grounded in the domain model
        assertTrue(specContent.contains("id:"), "MaterialDTO must include ID");
        assertTrue(specContent.contains("title:"), "MaterialDTO must include title");
        assertTrue(specContent.contains("content:"), "MaterialDTO must include content");
        assertTrue(specContent.contains("category:"), "MaterialDTO must include category");
        assertTrue(specContent.contains("status:"), "MaterialDTO must include status");
        assertTrue(specContent.contains("updated_at:"), "MaterialDTO must include updated_at");
    }
}
