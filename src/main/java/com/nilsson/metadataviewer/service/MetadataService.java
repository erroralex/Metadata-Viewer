package com.nilsson.metadataviewer.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified service for extracting AI metadata across all major platforms.
 * Supports: ComfyUI, SwarmUI, A1111, Forge, Reforge, and SD-Matrix.
 */
public class MetadataService {

    // Enable ALLOW_NON_NUMERIC_NUMBERS to handle 'NaN' tokens frequently found in ComfyUI
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();
        String rawData = extractRawMetadata(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        // Detect JSON-based formats (ComfyUI / SwarmUI / Matrix-JSON)
        if (trimmed.startsWith("{") || (trimmed.startsWith("\"") && trimmed.contains("\"prompt\""))) {
            parseJsonMetadata(trimmed, results);
        }
        // Detect Parameter-based formats (A1111 / Forge / Reforge / Matrix-Text)
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            parseA1111Format(rawData, results);
        }
        else {
            results.put("Prompt", rawData);
        }

        return results;
    }

    /**
     * Scans for metadata tags across all directories.
     * Prioritizes 'parameters' (A1111/Forge) and 'prompt/workflow' (ComfyUI).
     */
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

                    // Standard AI Metadata tags
                    if (name.contains("parameters")) parameters = desc;
                    else if (name.equals("prompt")) prompt = desc;
                    else if (name.equals("workflow")) workflow = desc;
                    else if (name.contains("user comment")) comment = desc;

                        // ComfyUI specific chunk detection within "Textual Data"
                    else if (name.contains("textual data") && desc.contains(": {")) {
                        String jsonPart = desc.substring(desc.indexOf("{")).trim();
                        // Only return if it actually looks like JSON to avoid false positives
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
            // Handle escaped JSON strings if the chunk was double-stringified
            String cleanJson = json;
            if (json.startsWith("\"") && json.endsWith("\"")) {
                cleanJson = json.substring(1, json.length() - 1).replace("\\\"", "\"");
            }

            JsonNode root = mapper.readTree(cleanJson);
            results.put("Type", "ComfyUI/SwarmUI/JSON");

            findKeysRecursively(root, results);

            if (!results.containsKey("Prompt") || results.get("Prompt").isEmpty()) {
                results.put("Prompt", findLongestText(root));
            }
        } catch (Exception e) {
            results.put("Prompt", "Error parsing JSON metadata: " + e.getMessage());
        }
    }

    /**
     * Aggressive recursive search for generation parameters.
     * Now includes support for UNET loaders and VAE loaders found in custom workflows.
     */
    private void findKeysRecursively(JsonNode node, Map<String, String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();

                // 1. PRIMARY MODEL DETECTION (Added 'model' for SwarmUI)
                boolean isTrueModelKey = key.equals("ckpt_name") ||
                        key.equals("unet_name") ||
                        key.equals("model_name") ||
                        key.contains("checkpoint") ||
                        key.equals("model_file") ||
                        key.equals("model"); // Added for SUI

                if (isTrueModelKey && value.isTextual()) {
                    String modelName = value.asText();
                    if (modelName.length() > 4 && !modelName.contains("{")) {
                        results.put("Model", modelName);
                    }
                }

                // 2. VAE DETECTION
                else if (key.equals("vae_name") && value.isTextual() && !results.containsKey("Model")) {
                    results.put("Model", value.asText());
                }

                // 3. SAMPLER & SCHEDULER (Added 'sampler' for SwarmUI)
                else if ((key.equals("sampler_name") || key.equals("sampler")) && value.isTextual()) {
                    String sampler = value.asText();
                    String scheduler = node.has("scheduler") ? node.get("scheduler").asText() : "";
                    results.put("Sampler", scheduler.isEmpty() ? sampler : sampler + " (" + scheduler + ")");
                }

                // 4. GENERATION STATS
                else if (key.equals("steps") && !results.containsKey("Steps")) {
                    results.put("Steps", value.asText());
                }
                else if (key.equals("seed") && !results.containsKey("Seed")) {
                    results.put("Seed", value.asText());
                }
                else if ((key.equals("cfg") || key.equals("cfgscale")) && !results.containsKey("CFG")) {
                    results.put("CFG", value.asText());
                }

                // 5. PROMPT DETECTION (Added 'prompt' and 'negativeprompt' for SwarmUI)
                else if ((key.equals("text") || key.equals("prompt")) && value.isTextual()) {
                    String t = value.asText();
                    if (t.length() > 5 && !t.startsWith("{") && !t.startsWith("[")) {
                        // Check if it's explicitly marked as negative or if the key is 'negativeprompt'
                        if (key.equals("negativeprompt") || (node.has("_meta") && node.get("_meta").has("title") &&
                                node.get("_meta").get("title").asText().toLowerCase().contains("negative"))) {
                            results.put("Negative", t);
                        } else if (!results.containsKey("Prompt")) {
                            results.put("Prompt", t);
                        }
                    }
                }
                else if (key.equals("negativeprompt") && value.isTextual()) {
                    results.put("Negative", value.asText());
                }

                // 6. LORA TRACKING
                else if (key.contains("lora_name") && value.isTextual()) {
                    String existing = results.getOrDefault("Loras", "");
                    String current = value.asText();
                    if (!existing.contains(current)) {
                        results.put("Loras", existing.isEmpty() ? current : existing + ", " + current);
                    }
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

    private void parseA1111Format(String raw, Map<String, String> results) {
        String[] sections = raw.split("\nSteps:");
        String promptPart = sections[0];

        // Robust Positive/Negative Split
        if (promptPart.contains("Negative prompt:")) {
            String[] split = promptPart.split("Negative prompt:");
            results.put("Prompt", split[0].trim());
            results.put("Negative", split[1].trim());
        } else {
            results.put("Prompt", promptPart.trim());
        }

        if (sections.length > 1) {
            String footer = "Steps:" + sections[1];
            results.put("Model", extractRegex(footer, "Model: ([^,]+)"));
            results.put("Steps", extractRegex(footer, "Steps: ([^,]+)"));
            results.put("CFG", extractRegex(footer, "CFG scale: ([^,]+)"));
            results.put("Seed", extractRegex(footer, "Seed: ([^,]+)"));

            // Capture Sampler and Scheduler for Reforge/Forge compatibility
            String sampler = extractRegex(footer, "Sampler: ([^,]+)");
            String scheduler = extractRegex(footer, "Schedule type: ([^,]+)");
            results.put("Sampler", (scheduler.equals("N/A") || scheduler.isEmpty()) ? sampler : sampler + " (" + scheduler + ")");

            // Lora Extraction
            Pattern p = Pattern.compile("<lora:([^:]+):");
            Matcher m = p.matcher(raw);
            java.util.Set<String> loras = new java.util.LinkedHashSet<>();
            while (m.find()) loras.add(m.group(1));
            results.put("Loras", loras.isEmpty() ? "None" : String.join(", ", loras));
        }
    }

    private String extractRegex(String src, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(src);
        return m.find() ? m.group(1).trim() : "N/A";
    }
}