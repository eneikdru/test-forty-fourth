package com.epitrack;

import com.epitrack.model.Category;
import com.epitrack.model.Material;
import com.epitrack.repository.CategoryRepository;
import com.epitrack.repository.MaterialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EpiTrackApplicationTests {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Test
    void contextLoads() {
        // Simple context loading check
    }

    @Test
    void testCategoryAndMaterialPersistence() {
        // 1. Create a Category and persist it
        Category category = new Category("Viral Strains", "All viral agents and samples");
        Category savedCategory = categoryRepository.save(category);

        assertNotNull(savedCategory.getId());
        assertEquals("Viral Strains", savedCategory.getName());
        assertEquals("All viral agents and samples", savedCategory.getDescription());

        // 2. Create a Material referencing the persisted Category
        LocalDateTime dateAdded = LocalDateTime.of(2026, 8, 12, 16, 5, 30);
        Material material = new Material("EP-2026-A01", "SARS-CoV-2 Batch A", savedCategory, dateAdded, "Analyzed");
        Material savedMaterial = materialRepository.save(material);

        assertEquals("EP-2026-A01", savedMaterial.getId());
        assertEquals("SARS-CoV-2 Batch A", savedMaterial.getName());
        assertEquals(savedCategory, savedMaterial.getCategory());
        assertEquals(dateAdded, savedMaterial.getDateAdded());
        assertEquals("Analyzed", savedMaterial.getStatus());

        // 3. Retrieve from database and verify attributes and associations
        Optional<Material> retrievedOpt = materialRepository.findById("EP-2026-A01");
        assertTrue(retrievedOpt.isPresent());

        Material retrievedMaterial = retrievedOpt.get();
        assertEquals("EP-2026-A01", retrievedMaterial.getId());
        assertEquals("SARS-CoV-2 Batch A", retrievedMaterial.getName());
        assertNotNull(retrievedMaterial.getCategory());
        assertEquals(savedCategory.getId(), retrievedMaterial.getCategory().getId());
        assertEquals("Viral Strains", retrievedMaterial.getCategory().getName());
        assertEquals(dateAdded, retrievedMaterial.getDateAdded());
        assertEquals("Analyzed", retrievedMaterial.getStatus());
    }
}
