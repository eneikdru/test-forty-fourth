package com.eneik.production;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaterialsApiContractTest {

    @Test
    public void testMaterialsApiContractStructureAndElements() throws Exception {
        // Load the OpenAPI contract YAML from the classpath/resources or as stream relative to the project root
        InputStream apiStream = getClass().getClassLoader().getResourceAsStream("docs/contracts/materials_api.yaml");
        if (apiStream == null) {
            // Fallback for different build environments or project structures
            apiStream = getClass().getResourceAsStream("/docs/contracts/materials_api.yaml");
        }
        if (apiStream == null) {
            // Fallback for file system based load if classpath lookup needs help
            java.io.File file = new java.io.File("docs/contracts/materials_api.yaml");
            if (file.exists()) {
                apiStream = new java.io.FileInputStream(file);
            }
        }

        assertNotNull(apiStream, "OpenAPI materials_api.yaml contract must be present.");

        String specContent;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(apiStream, StandardCharsets.UTF_8))) {
            specContent = reader.lines().collect(Collectors.joining("\n"));
        }

        // Validate essential OpenAPI elements of the contract
        assertTrue(specContent.contains("openapi: 3.0.3"), "Contract must be OpenAPI 3.0.3");
        assertTrue(specContent.contains("/api/materials:"), "Contract must define the /api/materials endpoint");
        assertTrue(specContent.contains("/api/materials/{id}:"), "Contract must define the /api/materials/{id} endpoint");
        assertTrue(specContent.contains("post:"), "Contract must define POST operation");
        assertTrue(specContent.contains("get:"), "Contract must define GET operation");
        assertTrue(specContent.contains("put:"), "Contract must define PUT operation");
        assertTrue(specContent.contains("delete:"), "Contract must define DELETE operation");

        // Validate Schema definition presence
        assertTrue(specContent.contains("MaterialDTO"), "Contract must define a MaterialDTO schema");
        assertTrue(specContent.contains("CreateMaterialRequest"), "Contract must define a CreateMaterialRequest schema");
        assertTrue(specContent.contains("UpdateMaterialRequest"), "Contract must define an UpdateMaterialRequest schema");
        assertTrue(specContent.contains("ErrorResponse"), "Contract must define an ErrorResponse schema");

        // Validate the DTO properties are grounded in the domain model
        assertTrue(specContent.contains("id:"), "MaterialDTO must include ID");
        assertTrue(specContent.contains("title:"), "MaterialDTO/CreateMaterialRequest must include title");
        assertTrue(specContent.contains("content:"), "MaterialDTO/CreateMaterialRequest must include content");
        assertTrue(specContent.contains("category:"), "MaterialDTO/CreateMaterialRequest must include category");
        assertTrue(specContent.contains("status:"), "MaterialDTO/CreateMaterialRequest must include status");
        assertTrue(specContent.contains("updated_at:"), "MaterialDTO must include updated_at");
    }
}
