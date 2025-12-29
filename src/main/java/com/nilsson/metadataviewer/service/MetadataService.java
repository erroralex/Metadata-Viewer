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

        // 1. Extract physical dimensions first (so we always have them)
        extractPhysicalDimensions(file, results);

        // 2. Find the BEST metadata string by analyzing content
        String rawData = findBestMetadataChunk(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        // 3. Process the chosen data
        // JSON Pipeline (ComfyUI, SwarmUI)
        if (trimmed.startsWith("{") || (trimmed.startsWith("\"") && trimmed.contains("\"prompt\""))) {
            parseJsonMetadata(trimmed, results);
        }
        // Text Pipeline (A1111, Forge)
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            textParser.parse(rawData, results);
            results.put("Software", "A1111 / Forge");
        }
        else {
            results.put("Prompt", rawData);
            results.put("Software", "Unknown");
        }

        return results;
    }

    /**
     * Scans the file for all text chunks and picks the one that looks like valid Generation Data.
     */
    private String findBestMetadataChunk(File file) {
        List<String> candidates = new ArrayList<>();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String desc = tag.getDescription();
                    if (desc == null) continue;

                    // Collect anything that looks like parameters or JSON
                    if (tag.getTagName().toLowerCase().contains("parameters") ||
                            tag.getTagName().toLowerCase().contains("user comment") ||
                            desc.contains("Steps:")) {
                        candidates.add(desc);
                    }
                    else if (desc.contains("{")) {
                        // Extract JSON part from "key: {json}" format
                        int braceIndex = desc.indexOf("{");
                        if (braceIndex != -1) {
                            candidates.add(desc.substring(braceIndex).trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        // SCORING SYSTEM: Find the "Winner" chunk
        String bestChunk = null;
        int bestScore = -1;

        for (String chunk : candidates) {
            int score = scoreChunk(chunk);
            if (score > bestScore) {
                bestScore = score;
                bestChunk = chunk;
            }
        }
        return bestChunk;
    }

    /**
     * Rates a metadata chunk based on how useful it is.
     * 100 = SwarmUI / ComfyUI API (Gold Standard)
     * 50  = A1111 Text Block
     * 10  = ComfyUI Workflow (Visual Graph - Use only as fallback)
     * 0   = Garbage
     */
    private int scoreChunk(String chunk) {
        if (chunk == null) return 0;

        // SwarmUI (Highest Priority)
        if (chunk.contains("sui_image_params")) return 100;

        // ComfyUI API (Look for numeric node IDs like "3": { "class_type": ... })
        // Use a regex to see if it starts with a number key
        if (chunk.matches("\\s*\\{\\s*\"\\d+\"\\s*:\\s*\\{.*")) return 90;

        // A1111 / Forge
        if (chunk.contains("Steps:") && chunk.contains("Sampler:")) return 80;

        // ComfyUI Workflow (Graph) - Low priority fallback
        if (chunk.contains("\"nodes\"") && chunk.contains("\"links\"")) return 10;

        return 0;
    }

    private void extractPhysicalDimensions(File file, Map<String, String> results) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String name = tag.getTagName().toLowerCase();
                    if (name.equals("image width")) results.put("Width", tag.getDescription().split(" ")[0]);
                    else if (name.equals("image height")) results.put("Height", tag.getDescription().split(" ")[0]);
                }
            }
        } catch (Exception ignored) {}
    }

    private void parseJsonMetadata(String json, Map<String, String> results) {
        try {
            String cleanJson = json;
            int lastBrace = cleanJson.lastIndexOf("}");
            if (lastBrace != -1 && lastBrace < cleanJson.length() - 1) {
                cleanJson = cleanJson.substring(0, lastBrace + 1);
            }
            if (cleanJson.startsWith("\"")) {
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1).replace("\\\"", "\"");
            }

            JsonNode root = mapper.readTree(cleanJson);

            // Determine Software based on the JSON structure we just validated
            String software = "Unknown";
            if (root.has("sui_image_params")) software = "SwarmUI";
            else if (root.has("meta") && root.get("meta").has("invokeai_metadata")) software = "InvokeAI";
            else if (root.has("uc")) software = "NovelAI";
            else {
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
            results.put("Prompt", "Error parsing JSON: " + e.getMessage());
        }
    }

    private void findKeysRecursively(JsonNode node, Map<String, String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                for (MetadataStrategy strategy : jsonStrategies) {
                    strategy.extract(entry.getKey().toLowerCase(), entry.getValue(), node, results);
                }
                findKeysRecursively(entry.getValue(), results);
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