package com.nilsson.metadataviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServiceTest {

    private final MetadataService service = new MetadataService();

    @Test
    void getExtractedData_throwsForMissingFile() {
        File missing = new File("does-not-exist.png");
        assertThrows(MetadataExtractionException.class, () -> service.getExtractedData(missing));
    }

    @Test
    void processRawMetadata_handlesNullRawData() {
        Map<String, String> result = service.processRawMetadata(null);
        assertEquals("No metadata found in this image.", result.get("Prompt"));
    }

    @Test
    void processRawMetadata_detectsA1111TextBlock(@TempDir Path tempDir) {
        String raw = "a cat on a windowsill\nSteps: 15, Sampler: Euler a, CFG scale: 6, Seed: 1";
        Map<String, String> result = service.processRawMetadata(raw);

        assertEquals("A1111 / Forge", result.get("Software"));
        assertEquals("15", result.get("Steps"));
    }
}
