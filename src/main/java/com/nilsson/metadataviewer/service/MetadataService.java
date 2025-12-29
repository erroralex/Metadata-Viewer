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

/**
 * Unified service for extracting AI metadata.
 * Acts as a Coordinator that dispatches extraction tasks to specific Strategies or Parsers.
 */
public class MetadataService {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

    // 1. The JSON Specialists (Strategy Pattern)
    private final List<MetadataStrategy> jsonStrategies = Arrays.asList(
            new SwarmUIStrategy(),
            new ComfyUIStrategy(),
            new InvokeAIStrategy(),
            new NovelAIStrategy(),
            new CommonStrategy()
    );

    // 2. The Text Specialist (Separated Parser)
    private final TextParamsParser textParser = new TextParamsParser();

    /**
     * Main Entry Point.
     * Detects the metadata format and delegates to the correct pipeline.
     */
    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();
        String rawData = extractRawMetadata(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        // -------------------------------------------------------
        // PIPELINE 1: JSON (ComfyUI, SwarmUI, InvokeAI, NovelAI)
        // -------------------------------------------------------
        if (trimmed.startsWith("{") || (trimmed.startsWith("\"") && trimmed.contains("\"prompt\""))) {
            parseJsonMetadata(trimmed, results);
        }
        // -------------------------------------------------------
        // PIPELINE 2: TEXT (A1111, Forge, Reforge, Fooocus)
        // -------------------------------------------------------
        // Checks for standard A1111 signature or Scheduler presence
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            textParser.parse(rawData, results);
        }
        // -------------------------------------------------------
        // FALLBACK: Raw Text
        // -------------------------------------------------------
        else {
            results.put("Prompt", rawData);
        }

        return results;
    }

    // Reads the physical file headers to find hidden metadata strings.
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
                        // ComfyUI specific chunk detection within "Textual Data"
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

    /**
     * Prepares the JSON and initiates the Recursive Strategy Search.
     */
    private void parseJsonMetadata(String json, Map<String, String> results) {
        try {
            // Handle double-escaped JSON strings if necessary
            String cleanJson = json;
            if (json.startsWith("\"") && json.endsWith("\"")) {
                cleanJson = json.substring(1, json.length() - 1).replace("\\\"", "\"");
            }

            JsonNode root = mapper.readTree(cleanJson);
            results.put("Type", "ComfyUI/SwarmUI/JSON"); // Generic Label

            findKeysRecursively(root, results);

            // Fallback: If no prompt found via strategies, try to find the longest text block
            if (!results.containsKey("Prompt") || results.get("Prompt").isEmpty()) {
                results.put("Prompt", findLongestText(root));
            }
        } catch (Exception e) {
            results.put("Prompt", "Error parsing JSON metadata: " + e.getMessage());
        }
    }

    /**
     * Aggressive Recursive Search.
     * Visits every node in the JSON tree and lets ALL active strategies try to extract data.
     */
    private void findKeysRecursively(JsonNode node, Map<String, String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();

                // EXECUTE ALL STRATEGIES ON THIS NODE
                for (MetadataStrategy strategy : jsonStrategies) {
                    strategy.extract(key, value, node, results);
                }

                // Continue Recursion
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