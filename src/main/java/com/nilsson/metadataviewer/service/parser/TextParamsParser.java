package com.nilsson.metadataviewer.service.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextParamsParser {

    // Pattern for A1111/Forge LoRA tags: <lora:MyModel_v1:0.8> or <lora:MyModel:1>
    private static final Pattern LORA_PATTERN = Pattern.compile("<lora:([^:>]+)(?::([^:>]+))?>", Pattern.CASE_INSENSITIVE);

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

        // 3. Extract LoRAs from the Positive Prompt (Crucial for Forge/A1111)
        extractLorasFromPrompt(positive, results);
    }

    private void parseParamLine(String line, Map<String, String> results) {
        Map<String, String> map = new HashMap<>();
        // Split by comma, but be careful not to split inside quotes if any (simple split is usually safe for A1111)
        String[] pairs = line.split(",\\s*");
        for (String pair : pairs) {
            String[] kv = pair.split(":\\s+", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }

        // --- Core Params ---
        if (map.containsKey("Model")) results.put("Model", map.get("Model"));
        if (map.containsKey("Steps")) results.put("Steps", map.get("Steps"));
        if (map.containsKey("Seed")) results.put("Seed", map.get("Seed"));
        if (map.containsKey("Size")) results.put("Size", map.get("Size"));
        if (map.containsKey("Model hash")) results.put("Model Hash", map.get("Model hash"));

        // --- Sampler & Scheduler ---
        if (map.containsKey("Sampler")) {
            String sampler = map.get("Sampler");
            if (map.containsKey("Schedule type")) {
                sampler += " (" + map.get("Schedule type") + ")";
            }
            results.put("Sampler", sampler);
        }

        // --- CFG & Distilled CFG (Flux Support) ---
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
}