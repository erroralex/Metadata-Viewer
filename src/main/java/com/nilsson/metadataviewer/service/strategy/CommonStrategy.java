package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class CommonStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isValueNode()) return;

        String text = value.asText();

        // 1. Steps
        if (key.equals("steps") && !results.containsKey("Steps")) {
            results.put("Steps", text);
        }

        // 2. Seed (Handle both 'seed' and 'noise_seed')
        else if ((key.equals("seed") || key.equals("noise_seed")) && !results.containsKey("Seed")) {
            results.put("Seed", text);
        }

        // 3. CFG Scale
        else if ((key.equals("cfg") || key.equals("cfgscale")) && !results.containsKey("CFG")) {
            results.put("CFG", text);
        }

        // 4. LoRA
        else if (key.contains("lora_name")) {
            String existing = results.getOrDefault("Loras", "");
            if (!existing.contains(text)) {
                results.put("Loras", existing.isEmpty() ? text : existing + ", " + text);
            }
        }

        // 5. Hires / Upscale
        else if (key.equals("upscale_by") || key.equals("upscale_method")) {
            results.put("Hires. fix", "Enabled (" + text + "x)");
        }

        // 6. ControlNet
        else if (key.contains("control_net") || key.contains("controlnet")) {
            String existing = results.getOrDefault("ControlNet", "");
            if (!existing.contains(text)) {
                results.put("ControlNet", existing.isEmpty() ? text : existing + ", " + text);
            }
        }
    }
}