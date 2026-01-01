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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();

        // 1. Extract physical dimensions (First priority)
        extractPhysicalDimensions(file, results);

        // 2. Find Metadata
        String rawData = findBestMetadataChunk(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        // 3. Process Data
        if (trimmed.startsWith("{") || (trimmed.startsWith("\"") && trimmed.contains("\"prompt\""))) {
            parseJsonMetadata(trimmed, results);
        }
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            results.putAll(TextParamsParser.parse(rawData));
            results.put("Software", "A1111 / Forge");
        }
        else {
            results.put("Prompt", rawData);
            results.put("Software", "Unknown");
        }

        return results;
    }

    private void extractPhysicalDimensions(File file, Map<String, String> results) {
        int width = 0;
        int height = 0;

        // METHOD 1: Fast Metadata Reading
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String name = tag.getTagName().toLowerCase();
                    String desc = tag.getDescription();
                    if (desc == null || desc.isEmpty()) continue;

                    if (name.contains("thumbnail") || name.contains("resolution")) continue;

                    String valStr = desc.split(" ")[0];
                    if (!valStr.matches("\\d+")) continue;

                    int val = Integer.parseInt(valStr);
                    if (val <= 0) continue;

                    if (name.contains("width") && val > width) width = val;
                    if (name.contains("height") && val > height) height = val;
                }
            }
        } catch (Exception ignored) {}

        // METHOD 2: Fallback to ImageIO
        if (width == 0 || height == 0) {
            try {
                BufferedImage bimg = ImageIO.read(file);
                if (bimg != null) {
                    width = bimg.getWidth();
                    height = bimg.getHeight();
                }
            } catch (Exception ignored) { }
        }

        if (width > 0) results.put("Width", String.valueOf(width));
        if (height > 0) results.put("Height", String.valueOf(height));
    }

    private String findBestMetadataChunk(File file) {
        List<String> candidates = new ArrayList<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String desc = tag.getDescription();
                    if (desc == null) continue;

                    if (tag.getTagName().toLowerCase().contains("parameters") ||
                            tag.getTagName().toLowerCase().contains("user comment") ||
                            desc.contains("Steps:")) {
                        candidates.add(desc);
                    }
                    else if (desc.contains("{")) {
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

    private int scoreChunk(String chunk) {
        if (chunk == null) return 0;
        if (chunk.contains("sui_image_params")) return 100;
        if (chunk.matches("(?s).*\\{\\s*\"\\d+\"\\s*:\\s*\\{.*")) return 90;
        if (chunk.contains("Steps:") && chunk.contains("Sampler:")) return 80;
        if (chunk.contains("\"nodes\"") && chunk.contains("\"links\"")) return 10;
        return 0;
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