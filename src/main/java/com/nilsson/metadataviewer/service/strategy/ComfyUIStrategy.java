package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class ComfyUIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isTextual()) return;
        String text = value.asText();

        // 1. Detect Models
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

        // 3. Prompts
        else if (key.equals("text") && text.length() > 5 && !text.startsWith("{")) {
            if (isNegativeNode(parentNode)) {
                results.put("Negative", text);
            } else if (!results.containsKey("Prompt")) {
                results.put("Prompt", text);
            }
        }

        // 4. Custom LoRA Loader Support (Power Lora Loader / rgthree)
        // Structure: { "lora": "Name.safetensors", "on": true, "strength": 1 }
        else if (key.equals("lora")) {
            // Check if 'on' is present and true (or missing, assuming on)
            boolean isOn = !parentNode.has("on") || parentNode.get("on").asBoolean();
            if (isOn) {
                addLora(results, text);
            }
        }
    }

    private void addLora(Map<String, String> results, String loraName) {
        // Clean up extension
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

    private boolean isNegativeNode(JsonNode node) {
        // Check Metadata Title (User defined or Default)
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            return title.contains("negative") || title.contains("neg ") || title.contains("(neg)") || title.equals("neg");
        }

        // Fallback: Check Inputs for standard negative labeling (rare but possible in custom nodes)
        if (node.has("inputs")) {
            JsonNode inputs = node.get("inputs");
            // Some custom nodes might use specific keys for negative text input
            if (inputs.has("negative") || inputs.has("neg")) {
                return true;
            }
        }

        return false;
    }
}