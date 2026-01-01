package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonStrategy implements MetadataStrategy {

    // --- 1. Text Parsing (Automatic1111 / Hive) ---
    public Map<String, String> parse(String text) {
        Map<String, String> results = new HashMap<>();

        String[] parts = text.split("Negative prompt:");
        String positivePrompt = parts[0].trim();
        results.put("Prompt", positivePrompt);

        String remaining = "";
        if (parts.length > 1) {
            String[] negAndParams = parts[1].split("\nSteps: ");
            results.put("Negative", negAndParams[0].trim());

            if (negAndParams.length > 1) {
                remaining = "Steps: " + negAndParams[1];
            } else {
                int lastSteps = parts[1].lastIndexOf("\nSteps:");
                if (lastSteps != -1) {
                    results.put("Negative", parts[1].substring(0, lastSteps).trim());
                    remaining = parts[1].substring(lastSteps + 1);
                } else {
                    remaining = parts[1];
                }
            }
        } else {
            int stepsIndex = text.lastIndexOf("\nSteps:");
            if (stepsIndex != -1) {
                results.put("Prompt", text.substring(0, stepsIndex).trim());
                remaining = text.substring(stepsIndex + 1);
            } else {
                remaining = text;
            }
        }

        extractLorasFromText(positivePrompt, results);

        Pattern paramPattern = Pattern.compile("([^:,]+):\\s*([^,]+)(?:,|$)");
        Matcher matcher = paramPattern.matcher(remaining);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();

            switch (key) {
                case "Steps" -> results.put("Steps", value);
                case "Sampler" -> results.put("Sampler", value);
                case "CFG scale" -> results.put("CFG", value);
                case "Seed" -> results.put("Seed", value);
                case "Size" -> {
                    String[] dim = value.split("x");
                    if (dim.length == 2) {
                        results.put("Width", dim[0]);
                        results.put("Height", dim[1]);
                    }
                }
                case "Model" -> results.put("Model", value);
                case "Model hash" -> results.put("Model Hash", value);
                case "Denoising strength" -> results.put("Denoise", value);
                case "Hires upscale" -> results.put("Hires. fix", "Enabled (" + value + "x)");
                case "Lora hashes" -> {
                    if (!results.containsKey("Loras")) {
                        results.put("Loras", value);
                    }
                }
            }
        }

        return results;
    }

    private void extractLorasFromText(String prompt, Map<String, String> results) {
        Pattern loraPattern = Pattern.compile("<lora:([^:>]+)(?::([^:>]+))?(?::([^:>]+))?>", Pattern.CASE_INSENSITIVE);
        Matcher m = loraPattern.matcher(prompt);
        StringBuilder loraBuilder = new StringBuilder();

        while (m.find()) {
            String name = m.group(1);
            String strength = m.group(2);

            if (loraBuilder.length() > 0) loraBuilder.append(", ");
            loraBuilder.append(name);
            if (strength != null) loraBuilder.append(" (").append(strength).append(")");
        }

        if (loraBuilder.length() > 0) {
            results.put("Loras", loraBuilder.toString());
        }
    }

    // --- 2. JSON Extraction (Fallback) ---
    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isValueNode()) return;
        String text = value.asText().trim();
        if (text.isEmpty()) return;

        if (key.equals("steps")) results.put("Steps", text);
        else if (key.equals("seed") || key.equals("noise_seed")) results.put("Seed", text);
        else if (key.equals("cfg") || key.equals("cfgscale")) results.put("CFG", text);

            // Only extract Width/Height if valid (> 0). This prevents overwriting
            // valid physical dimensions with "0" from generic nodes
        else if (key.equals("width")) {
            if (isValidSize(text)) results.put("Width", text);
        }
        else if (key.equals("height")) {
            if (isValidSize(text)) results.put("Height", text);
        }

        else if (key.contains("lora_name")) {
            String existing = results.getOrDefault("Loras", "");
            if (!existing.contains(text)) {
                results.put("Loras", existing.isEmpty() ? text : existing + ", " + text);
            }
        }
        else if (key.equals("upscale_by") || key.equals("upscale_method")) {
            results.put("Hires. fix", "Enabled (" + text + "x)");
        }
        else if (key.contains("control_net") || key.contains("controlnet")) {
            String existing = results.getOrDefault("ControlNet", "");
            if (!existing.contains(text)) {
                results.put("ControlNet", existing.isEmpty() ? text : existing + ", " + text);
            }
        }
    }

    private boolean isValidSize(String text) {
        try {
            int val = Integer.parseInt(text);
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}