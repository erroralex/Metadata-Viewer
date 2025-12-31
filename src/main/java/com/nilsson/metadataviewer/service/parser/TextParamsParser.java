package com.nilsson.metadataviewer.service.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParamsParser {

    // Pattern for A1111/Forge LoRA tags
    private static final Pattern LORA_PATTERN = Pattern.compile("<lora:([^:>]+)(?::([^:>]+))?>", Pattern.CASE_INSENSITIVE);

    // Pattern to find the Civitai Resources JSON block
    private static final Pattern CIVITAI_BLOCK_PATTERN = Pattern.compile("Civitai resources:\\s*(\\[.*?\\])");

    // Pattern to extract Model Name from a JSON-like object string
    private static final Pattern CIVITAI_MODEL_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"checkpoint\".*?\"modelName\"\\s*:\\s*\"([^\"]+)\"");

    public void parse(String rawData, Map<String, String> results) {
        if (rawData == null || rawData.isEmpty()) return;

        // 1. Split into chunks (Positive, Negative, Params)
        String[] parts = rawData.split("\nNegative prompt: ");
        String positive = parts[0].trim();
        String negative = "";
        String params = "";

        if (parts.length > 1) {
            String[] negParts = parts[1].split("\nSteps: ");
            negative = negParts[0].trim();
            if (negParts.length > 1) {
                params = "Steps: " + negParts[1].trim();
            }
        } else {
            // Case where there might be no negative prompt but still params
            String[] paramSplit = rawData.split("\nSteps: ");
            if (paramSplit.length > 1) {
                positive = paramSplit[0].trim();
                params = "Steps: " + paramSplit[1].trim();
            }
        }

        results.put("Prompt", positive);
        results.put("Negative", negative);

        // 2. Parse Standard Parameters (Steps, Sampler, etc.)
        if (!params.isEmpty()) {
            parseParamLine(params, results);
        }

        // 3. Extract LoRAs from the Positive Prompt
        extractLorasFromPrompt(positive, results);

        // 4. Fallback: Extract Model from Civitai Resources if missing
        if (!results.containsKey("Model")) {
            extractCivitaiResources(rawData, results);
        }
    }

    private void parseParamLine(String line, Map<String, String> results) {
        Map<String, String> map = new HashMap<>();

        // Note: The simple split by comma works for standard params.
        // It might fragment the JSON at the end, but that's fine because we extract JSON separately via Regex.
        String[] pairs = line.split(",\\s*");
        for (String pair : pairs) {
            String[] kv = pair.split(":\\s+", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }

        if (map.containsKey("Model")) results.put("Model", map.get("Model"));
        if (map.containsKey("Steps")) results.put("Steps", map.get("Steps"));
        if (map.containsKey("Seed")) results.put("Seed", map.get("Seed"));
        if (map.containsKey("Size")) results.put("Size", map.get("Size"));
        if (map.containsKey("Model hash")) results.put("Model Hash", map.get("Model hash"));
        if (map.containsKey("Clip skip")) results.put("Clip Skip", map.get("Clip skip"));

        // --- Sampler & Scheduler Logic ---
        if (map.containsKey("Sampler")) {
            String sampler = map.get("Sampler");
            String scheduler = "";

            if (map.containsKey("Schedule type")) {
                scheduler = map.get("Schedule type");
            } else if (map.containsKey("Scheduler")) {
                scheduler = map.get("Scheduler");
            }

            if (!scheduler.isEmpty()) {
                sampler += " (" + scheduler + ")";
            }
            results.put("Sampler", sampler);
        }

        // --- CFG & Distilled CFG ---
        if (map.containsKey("CFG scale")) {
            String cfg = map.get("CFG scale");
            if (map.containsKey("Distilled CFG Scale")) {
                cfg += " (Distilled: " + map.get("Distilled CFG Scale") + ")";
            }
            results.put("CFG", cfg);
        }
    }

    private void extractLorasFromPrompt(String prompt, Map<String, String> results) {
        Matcher m = LORA_PATTERN.matcher(prompt);
        StringBuilder loraBuilder = new StringBuilder();

        while (m.find()) {
            String name = m.group(1).trim();
            String strength = m.group(2); // Can be null

            String entry = name;
            if (strength != null && !strength.isEmpty() && !strength.equals("1") && !strength.equals("1.0")) {
                entry += " (" + strength + ")";
            }

            if (loraBuilder.length() > 0) loraBuilder.append(", ");
            loraBuilder.append(entry);
        }

        if (loraBuilder.length() > 0) {
            results.put("Loras", loraBuilder.toString());
        }
    }

    private void extractCivitaiResources(String rawData, Map<String, String> results) {
        Matcher blockMatcher = CIVITAI_BLOCK_PATTERN.matcher(rawData);
        if (blockMatcher.find()) {
            String jsonBlock = blockMatcher.group(1);
            Matcher modelMatcher = CIVITAI_MODEL_PATTERN.matcher(jsonBlock);
            if (modelMatcher.find()) {
                results.put("Model", modelMatcher.group(1).trim());
            }
        }
    }
}