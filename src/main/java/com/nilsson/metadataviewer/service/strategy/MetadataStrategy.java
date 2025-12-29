package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public interface MetadataStrategy {
    /**
     * Inspects a single JSON node and extracts relevant metadata.
     * @param key The JSON key (e.g., "steps", "model_name")
     * @param value The JSON value associated with the key
     * @param parentNode The parent object (useful for context like looking up 'scheduler' sibling)
     * @param results The map to populate with extracted data
     */
    void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results);
}