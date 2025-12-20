package com.nilsson.metadataviewer.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Service for extracting AI metadata
public class MetadataService {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();
        String rawData = extractRawMetadata(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);
        String trimmed = rawData.trim();

        if (trimmed.startsWith("{")) {
            parseJsonMetadata(trimmed, results);
        } else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            parseA1111Format(rawData, results);
        } else {
            results.put("Prompt", rawData);
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
            JsonNode root = mapper.readTree(json);
            results.put("Type", "ComfyUI/SwarmUI/JSON");

            findKeysRecursively(root, results);

            // Special fallback for Prompt if the recursive search missed it
            if (!results.containsKey("Prompt") || results.get("Prompt").isEmpty()) {
                results.put("Prompt", findLongestText(root));
            }
        } catch (Exception e) {
            results.put("Prompt", "Error parsing JSON metadata: " + e.getMessage());
        }
    }

    private void findKeysRecursively(JsonNode node, Map<String, String> results) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey().toLowerCase();
                JsonNode value = entry.getValue();

                // 1. Model Support (SwarmUI & ComfyUI)
                if ((key.equals("model") || key.equals("ckpt_name")) && !results.containsKey("Model")) {
                    results.put("Model", value.asText());
                }

                // 2. Generation Stats (Steps, Seed, CFG)
                else if (key.equals("steps") && !results.containsKey("Steps")) {
                    results.put("Steps", value.asText());
                }
                else if (key.equals("seed") && !results.containsKey("Seed")) {
                    results.put("Seed", value.asText());
                }
                else if (key.equals("cfgscale") || key.equals("cfg")) {
                    results.put("CFG", value.asText());
                }

                // 3. Sampler/Scheduler
                else if ((key.equals("sampler_name") || key.equals("sampler")) && !results.containsKey("Sampler")) {
                    String sampler = value.asText();
                    String scheduler = node.has("scheduler") ? node.get("scheduler").asText() : "";
                    results.put("Sampler", scheduler.isEmpty() ? sampler : sampler + " (" + scheduler + ")");
                }

                // 4. Prompts (Positive & Negative)
                else if (key.equals("prompt") && !results.containsKey("Prompt")) {
                    results.put("Prompt", value.asText().trim());
                }
                else if (key.equals("negativeprompt") || key.equals("negative_prompt")) {
                    results.put("Negative", value.asText().trim());
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
            if (val.length() > longest[0].length() && !val.startsWith("{")) {
                longest[0] = val;
            }
        });
        return longest[0];
    }

    private void parseA1111Format(String raw, Map<String, String> results) {
        String[] sections = raw.split("\nSteps:");
        String promptPart = sections[0];

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

            String sampler = extractRegex(footer, "Sampler: ([^,]+)");
            String scheduler = extractRegex(footer, "Schedule type: ([^,]+)");
            results.put("Sampler", (scheduler.equals("N/A") || scheduler.isEmpty()) ? sampler : sampler + " (" + scheduler + ")");

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