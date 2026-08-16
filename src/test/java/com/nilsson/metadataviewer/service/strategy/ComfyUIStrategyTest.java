package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComfyUIStrategyTest {

    private final ComfyUIStrategy strategy = new ComfyUIStrategy();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extract_resolvesParamsFromApiSchemaKSampler() throws Exception {
        // Minimal ComfyUI API-schema graph: CLIPTextEncode -> KSampler
        String json = """
                {
                  "1": {
                    "class_type": "CLIPTextEncode",
                    "inputs": { "text": "a neon city at night" }
                  },
                  "2": {
                    "class_type": "KSampler",
                    "inputs": {
                      "seed": 123456789,
                      "steps": 30,
                      "cfg": 7.5,
                      "sampler_name": "dpmpp_2m",
                      "scheduler": "karras",
                      "positive": ["1", 0]
                    }
                  }
                }
                """;

        JsonNode root = mapper.readTree(json);
        Map<String, String> results = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = fields.next();
            JsonNode node = nodeEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
            while (nodeFields.hasNext()) {
                Map.Entry<String, JsonNode> field = nodeFields.next();
                strategy.extract(field.getKey(), field.getValue(), node, results);
            }
        }

        assertEquals("30", results.get("Steps"));
        assertEquals("7.5", results.get("CFG"));
        assertEquals("dpmpp_2m", results.get("Sampler"));
    }
}
