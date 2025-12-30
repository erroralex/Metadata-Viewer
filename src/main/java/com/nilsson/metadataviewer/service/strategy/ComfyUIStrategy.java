package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.text.DecimalFormat;

public class ComfyUIStrategy implements MetadataStrategy {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {

        // 1. Context-Aware Input Block Handler
        if (key.equals("inputs") && value.isObject()) {
            processInputsBlock(value, parentNode, results);
            return;
        }

        if (!value.isTextual()) return;
        String text = value.asText();

        // --- Models ---
        if (isComfyModelKey(key) && text.length() > 4 && !text.contains("{")) {
            results.put("Model", text);
        }
        // --- VAE ---
        else if ((key.equals("vae_name") || key.equals("vae"))) {
            results.put("VAE", text);
        }
        // --- Sampler ---
        else if (key.equals("sampler_name")) {
            String scheduler = parentNode.has("scheduler") ? parentNode.get("scheduler").asText() : "";
            results.put("Sampler", scheduler.isEmpty() ? text : text + " (" + scheduler + ")");
        }
        // --- LoRA Extraction ---
        else if (key.equals("lora_name") || key.equals("lora")) {

            // Check "on" flag for Power Lora Loader (rgthree)
            if (parentNode.has("on") && !parentNode.get("on").asBoolean()) {
                return;
            }

            double strModel = 1.0;
            double strClip = 1.0;
            boolean foundStrength = false;

            // 1. Try standard LoRA Loader inputs
            if (parentNode.has("strength_model")) {
                strModel = parentNode.get("strength_model").asDouble();
                foundStrength = true;
            }
            if (parentNode.has("strength_clip")) {
                strClip = parentNode.get("strength_clip").asDouble();
                foundStrength = true;
            }

            // 2. Try generic "strength" (Power Lora / Simple Lora)
            if (!foundStrength && parentNode.has("strength")) {
                strModel = parentNode.get("strength").asDouble();
                strClip = strModel; // Assume applied to both if only one is present
            }

            String cleanName = text.replace(".safetensors", "").replace(".pt", "");
            String finalEntry = cleanName;

            // Format Strength string: "Name (0.8)" or "Name (M:0.8, C:0.5)"
            if (strModel == 1.0 && strClip == 1.0) {
                // No strength display needed
            } else if (strModel == strClip) {
                finalEntry += " (" + DF.format(strModel) + ")";
            } else {
                finalEntry += " (M:" + DF.format(strModel) + ", C:" + DF.format(strClip) + ")";
            }

            addLora(results, finalEntry);
        }
    }

    private void processInputsBlock(JsonNode inputs, JsonNode node, Map<String, String> results) {
        if (isNegativeNode(node, inputs)) {
            extractPromptText(inputs, results, "Negative");
        } else if (isPositiveNode(node)) {
            if (!results.containsKey("Prompt")) {
                extractPromptText(inputs, results, "Prompt");
            }
        }
    }

    private void extractPromptText(JsonNode inputs, Map<String, String> results, String targetKey) {
        String text = null;
        if (hasText(inputs, "text")) text = inputs.get("text").asText();
        else if (hasText(inputs, "text_g")) text = inputs.get("text_g").asText();
        else if (hasText(inputs, "text_l")) text = inputs.get("text_l").asText();
        else if (hasText(inputs, "string")) text = inputs.get("string").asText();

        if (text != null && text.length() > 1) {
            results.put(targetKey, text);
        }
    }

    private boolean hasText(JsonNode node, String field) {
        return node.has(field) && node.get(field).isTextual();
    }

    private boolean isNegativeNode(JsonNode node, JsonNode inputs) {
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            if (title.contains("negative") || title.contains("neg ") || title.contains("(neg)") || title.equals("neg")) return true;
        }
        return inputs.has("negative") || inputs.has("neg");
    }

    private boolean isPositiveNode(JsonNode node) {
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            return title.contains("positive") || title.contains("prompt");
        }
        return false;
    }

    private void addLora(Map<String, String> results, String loraString) {
        String existing = results.getOrDefault("Loras", "");
        if (existing.isEmpty() || existing.equals("None")) {
            results.put("Loras", loraString);
        } else if (!existing.contains(loraString)) {
            results.put("Loras", existing + ", " + loraString);
        }
    }

    private boolean isComfyModelKey(String key) {
        return key.equals("ckpt_name") || key.equals("unet_name") || key.equals("model_name") || key.contains("checkpoint");
    }
}