package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class ComfyUIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isTextual()) return;
        String text = value.asText();

        // 1. Detect Models (Checkpoints, UNETs, etc.)
        if (isComfyModelKey(key) && text.length() > 4 && !text.contains("{")) {
            results.put("Model", text);
        }
        // VAE Detection
        else if ((key.equals("vae_name") || key.equals("vae"))) {
            results.put("VAE", text);
        }

        // 2. Sampler & Scheduler
        else if (key.equals("sampler_name")) {
            String scheduler = parentNode.has("scheduler") ? parentNode.get("scheduler").asText() : "";
            results.put("Sampler", scheduler.isEmpty() ? text : text + " (" + scheduler + ")");
        }

        // 3. Prompts (Node text inputs)
        else if (key.equals("text") && text.length() > 5 && !text.startsWith("{")) {
            if (isNegativeNode(parentNode)) {
                results.put("Negative", text);
            } else if (!results.containsKey("Prompt")) {
                results.put("Prompt", text);
            }
        }
    }

    private boolean isComfyModelKey(String key) {
        return key.equals("ckpt_name") || key.equals("unet_name") ||
                key.equals("model_name") || key.contains("checkpoint") ||
                key.equals("model_file");
    }

    private boolean isNegativeNode(JsonNode node) {
        return node.has("_meta") && node.get("_meta").has("title") &&
                node.get("_meta").get("title").asText().toLowerCase().contains("negative");
    }
}