package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class ComfyUIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {

        // 1. Context-Aware Input Block Handler
        // We catch the 'inputs' object so we can check the Node's Title (parentNode)
        if (key.equals("inputs") && value.isObject()) {
            processInputsBlock(value, parentNode, results);
            return;
        }

        // 2. Standard Parameter Extraction (Scalar values)
        if (!value.isTextual()) return;
        String text = value.asText();

        // Models
        if (isComfyModelKey(key) && text.length() > 4 && !text.contains("{")) {
            results.put("Model", text);
        }
        // VAE
        else if ((key.equals("vae_name") || key.equals("vae"))) {
            results.put("VAE", text);
        }
        // Sampler
        else if (key.equals("sampler_name")) {
            String scheduler = parentNode.has("scheduler") ? parentNode.get("scheduler").asText() : "";
            results.put("Sampler", scheduler.isEmpty() ? text : text + " (" + scheduler + ")");
        }
        // Custom LoRA Loader (Power Lora / rgthree)
        else if (key.equals("lora")) {
            boolean isOn = !parentNode.has("on") || parentNode.get("on").asBoolean();
            if (isOn) {
                addLora(results, text);
            }
        }
    }

    /**
     * Inspects the "inputs" block of a node.
     * Since we are here, 'node' is the parent Node object (which contains _meta/class_type),
     * allowing us to correctly identify if this is a Positive or Negative prompt node.
     */
    private void processInputsBlock(JsonNode inputs, JsonNode node, Map<String, String> results) {
        // Detect Negative Node (by Title or Inputs)
        if (isNegativeNode(node, inputs)) {
            extractPromptText(inputs, results, "Negative");
        }
        // Detect Positive Node (by Title or Explicit exclusion of negative)
        else if (isPositiveNode(node)) {
            // Only add if we haven't found a prompt yet, or if this one clearly identifies as Positive
            if (!results.containsKey("Prompt")) {
                extractPromptText(inputs, results, "Prompt");
            }
        }
    }

    private void extractPromptText(JsonNode inputs, Map<String, String> results, String targetKey) {
        // Try common keys where text is stored.
        // We check isTextual() to avoid grabbing node links (arrays) like ["112", 0]
        String text = null;
        if (hasText(inputs, "text")) text = inputs.get("text").asText();
        else if (hasText(inputs, "text_g")) text = inputs.get("text_g").asText();
        else if (hasText(inputs, "text_l")) text = inputs.get("text_l").asText();
        else if (hasText(inputs, "string")) text = inputs.get("string").asText(); // Common in some custom nodes

        if (text != null && !text.isEmpty() && text.length() > 1) {
            results.put(targetKey, text);
        }
    }

    private boolean hasText(JsonNode node, String field) {
        return node.has(field) && node.get(field).isTextual();
    }

    private boolean isNegativeNode(JsonNode node, JsonNode inputs) {
        // 1. Check Metadata Title (User defined or Default)
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            if (title.contains("negative") || title.contains("neg ") || title.contains("(neg)") || title.equals("neg")) {
                return true;
            }
        }

        // 2. Fallback: Check for specific "negative" input keys (rare but used in some suites)
        if (inputs.has("negative") || inputs.has("neg")) {
            return true;
        }

        return false;
    }

    private boolean isPositiveNode(JsonNode node) {
        // Check for explicit Positive title to distinguish from random other text nodes
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            return title.contains("positive") || title.contains("prompt");
        }
        return false;
    }

    private void addLora(Map<String, String> results, String loraName) {
        String cleanName = loraName.replace(".safetensors", "").replace(".pt", "");
        String existing = results.getOrDefault("Loras", "");
        if (existing.isEmpty() || existing.equals("None")) {
            results.put("Loras", cleanName);
        } else if (!existing.contains(cleanName)) {
            results.put("Loras", existing + ", " + cleanName);
        }
    }

    private boolean isComfyModelKey(String key) {
        return key.equals("ckpt_name") || key.equals("unet_name") ||
                key.equals("model_name") || key.contains("checkpoint") ||
                key.equals("model_file");
    }
}