package com.nilsson.metadataviewer.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nilsson.metadataviewer.service.parser.TextParamsParser;
import com.nilsson.metadataviewer.service.strategy.*;

import java.io.File;
import java.util.*;

public class MetadataService {

    // Allow 'NaN' which appears in ComfyUI JSON
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    private final List<MetadataStrategy> jsonStrategies = Arrays.asList(
            new SwarmUIStrategy(),
            new ComfyUIStrategy(),
            new InvokeAIStrategy(),
            new NovelAIStrategy(),
            new CommonStrategy()
    );

    private final TextParamsParser textParser = new TextParamsParser();

    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();
        String rawData = extractRawMetadata(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        // Pipeline 1: JSON (ComfyUI, SwarmUI, etc.)
        if (trimmed.startsWith("{") || (trimmed.startsWith("\"") && trimmed.contains("\"prompt\""))) {
            parseJsonMetadata(trimmed, results);
        }
        // Pipeline 2: A1111 / Forge Text Block
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            textParser.parse(rawData, results);
            results.put("Software", "A1111 / Forge");
        }
        // Fallback
        else {
            results.put("Prompt", rawData);
            results.put("Software", "Unknown");
        }

        return results;
    }

    private String extractRawMetadata(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            String parameters = null;
            String prompt = null;
            String workflow = null;
            String comment = null;

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String name = tag.getTagName().toLowerCase();
                    String desc = tag.getDescription();

                    if (name.contains("parameters")) parameters = desc;
                    else if (name.equals("prompt")) prompt = desc;
                    else if (name.equals("workflow")) workflow = desc;
                    else if (name.contains("user comment")) comment = desc;
                    else if (name.contains("textual data") && desc.contains(": {")) {
                        String jsonPart = desc.substring(desc.indexOf("{")).trim();
                        if (jsonPart.startsWith("{")) return jsonPart;
                    }
                }
            }
            if (parameters != null) return parameters;
            if (prompt != null) return prompt;
            if (workflow != null) return workflow;
            return comment;
        } catch (Exception e) {
            return null;
        }
    }

    private void parseJsonMetadata(String json, Map<String, String> results) {
        try {
            String cleanJson = json;

            // Handle trailing text (e.g., "} Version: ComfyUI")
            int lastBrace = cleanJson.lastIndexOf("}");
            if (lastBrace != -1 && lastBrace < cleanJson.length() - 1) {
                cleanJson = cleanJson.substring(0, lastBrace + 1);
            }

            // Handle double-escaped strings
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).replace("\\\"", "\"");
            }

            JsonNode root = mapper.readTree(cleanJson);

            // Robust Software Detection Logic
            String software = "Unknown";

            if (root.has("sui_image_params")) {
                software = "SwarmUI";
            } else if (root.has("invokeai_metadata") || (root.has("meta") && root.get("meta").has("invokeai_metadata"))) {
                software = "InvokeAI";
            } else if (root.has("uc")) {
                software = "NovelAI";
            } else {
                // ComfyUI Heuristic: Check if keys are numbers ("3", "6") and values have "class_type"
                Iterator<String> keys = root.fieldNames();
                if (keys.hasNext()) {
                    String firstKey = keys.next();
                    if (firstKey.matches("\\d+") && root.get(firstKey).has("class_type")) {
                        software = "ComfyUI";
                    } else if (root.has("nodes") && root.has("links")) {
                        software = "ComfyUI (Workflow)";
                    }
                }
            }

            results.put("Software", software);

            findKeysRecursively(root, results);

            if (!results.containsKey("Prompt") || results.get("Prompt").isEmpty()) {
                results.put("Prompt", findLongestText(root));
            }
        } catch (Exception e) {
            results.put("Prompt", "Error parsing JSON metadata: " + e.getMessage());
            results.put("Software", "Error");
        }
    }

    private void findKeysRecursively(JsonNode node, Map<String, String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();

                for (MetadataStrategy strategy : jsonStrategies) {
                    strategy.extract(key, value, node, results);
                }
                findKeysRecursively(value, results);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                findKeysRecursively(child, results);
            }
        }
    }

    private String findLongestText(JsonNode node) {
        final String[] longest = {"No descriptive prompt found"};
        node.findValues("text").forEach(v -> {
            String val = v.asText();
            if (val.length() > longest[0].length() && !val.contains("{")) {
                longest[0] = val;
            }
        });
        return longest[0];
    }
}