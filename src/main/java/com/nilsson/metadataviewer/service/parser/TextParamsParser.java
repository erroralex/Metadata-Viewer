package com.nilsson.metadataviewer.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nilsson.metadataviewer.service.strategy.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TextParamsParser {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, String> parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new HashMap<>();
        }

        // 1. JSON Formats (ComfyUI)
        // Checked first to identify API formats that might contain "Steps:" text inside them
        if (text.trim().startsWith("{")) {
            try {
                JsonNode root = mapper.readTree(text);
                Map<String, String> results = new HashMap<>();
                ComfyUIStrategy strategy = new ComfyUIStrategy();

                // CASE A: Standard UI Workflow (Has "nodes" array)
                if (root.has("nodes")) {
                    for (JsonNode node : root.get("nodes")) {
                        processComfyNode(node, strategy, results);
                    }
                    // Pass the whole root for strategy to handle global links if needed
                    strategy.extract("nodes_wrapper", root, null, results);
                }
                // CASE B: API Format (Root keys are Node IDs "1", "2", ...)
                else {
                    boolean isApi = false;
                    Iterator<JsonNode> it = root.elements();
                    while (it.hasNext()) {
                        if (it.next().has("class_type")) {
                            isApi = true;
                            break;
                        }
                    }

                    if (isApi) {
                        // Pass the entire root to the strategy for API processing (resolving links)
                        strategy.extract("api_nodes", root, null, results);
                    } else {
                        // Fallback iteration
                        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> entry = fields.next();
                            JsonNode node = entry.getValue();
                            if (node.has("inputs") && node.has("class_type")) {
                                processComfyNode(node, strategy, results);
                            }
                        }
                    }
                }

                if (!results.isEmpty()) return results;

            } catch (Exception e) {
                // Not a valid ComfyUI JSON, fall through
            }
        }

        // 2. Automatic1111 / Hive (Standard Text-Based)
        if (text.contains("Steps: ") && text.contains("Sampler: ")) {
            return new CommonStrategy().parse(text);
        }

        // 3. InvokeAI
        if (text.contains("\"app_version\":") && text.contains("invokeai")) {
            return new InvokeAIStrategy().parse(text);
        }

        // 4. NovelAI
        if (text.contains("NovelAI")) {
            return new NovelAIStrategy().parse(text);
        }

        // 5. SwarmUI
        if (text.contains("sui_image_params")) {
            return new SwarmUIStrategy().parse(text);
        }

        return new HashMap<>();
    }

    private static void processComfyNode(JsonNode node, ComfyUIStrategy strategy, Map<String, String> results) {
        Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
        while (nodeFields.hasNext()) {
            Map.Entry<String, JsonNode> field = nodeFields.next();
            strategy.extract(field.getKey(), field.getValue(), node, results);
        }
    }
}