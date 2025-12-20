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


/**
 * Robust service for extracting AI metadata.
 * Optimized for A1111, Forge, ReForge, ComfyUI, and SwarmUI.
 */
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

        // 1. Detect JSON (ComfyUI / SwarmUI)
        if (trimmed.startsWith("{")) {
            parseJsonMetadata(trimmed, results);
        }
        // 2. Detect A1111 / Forge / ReForge / Swarm (Standard Parameters)
        else if (rawData.contains("Steps:") && (rawData.contains("Sampler:") || rawData.contains("Schedule type:"))) {
            parseA1111Format(rawData, results);
        }
        else {
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

                    // Priority order for AI metadata chunks
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
            results.put("Type", "ComfyUI/SwarmUI");

            // ComfyUI stores data in nodes. We iterate to find relevant generation info.
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode node = entry.getValue();

                if (node.has("class_type")) {
                    String type = node.get("class_type").asText();
                    JsonNode inputs = node.get("inputs");

                    // Extract Model
                    if (type.contains("CheckpointLoader") && inputs.has("ckpt_name")) {
                        results.put("Model", inputs.get("ckpt_name").asText());
                    }

                    // Extract Sampling Params (Steps, Sampler, Scheduler, Seed)
                    if (type.equals("KSampler") || type.equals("KSamplerAdvanced")) {
                        if (inputs.has("steps")) results.put("Steps", inputs.get("steps").asText());
                        if (inputs.has("sampler_name")) {
                            String s = inputs.get("sampler_name").asText();
                            String sch = inputs.has("scheduler") ? inputs.get("scheduler").asText() : "";
                            results.put("Sampler", sch.isEmpty() ? s : s + " (" + sch + ")");
                        }
                    }

                    // Extract Positive Prompt
                    if (type.equals("CLIPTextEncode") && inputs.has("text")) {
                        String text = inputs.get("text").asText();
                        // Usually, the first CLIPTextEncode is the positive prompt
                        if (!results.containsKey("Prompt") || results.get("Prompt").isEmpty()) {
                            results.put("Prompt", text);
                        }
                    }

                    // Lora Tracking
                    if (type.contains("LoraLoader") && inputs.has("lora_name")) {
                        String existing = results.getOrDefault("Loras", "");
                        String current = inputs.get("lora_name").asText();
                        results.put("Loras", existing.isEmpty() ? current : existing + ", " + current);
                    }
                }
            }

            // Fallback if specific nodes weren't found (using regex on the blob)
            if (!results.containsKey("Model")) results.put("Model", extractRegex(json, "\"ckpt_name\":\\s*\"([^\"]+)\""));
            if (!results.containsKey("Steps")) results.put("Steps", extractRegex(json, "\"steps\":\\s*(\\d+)"));

        } catch (Exception e) {
            results.put("Prompt", "Error parsing JSON metadata.");
        }
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

            String sampler = extractRegex(footer, "Sampler: ([^,]+)");
            String scheduler = extractRegex(footer, "Schedule type: ([^,]+)");
            results.put("Sampler", scheduler.equals("N/A") ? sampler : sampler + " (" + scheduler + ")");

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