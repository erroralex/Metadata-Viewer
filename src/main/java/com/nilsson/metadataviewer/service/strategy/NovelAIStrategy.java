package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class NovelAIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isTextual()) return;
        String text = value.asText();

        // 1. Prompts
        if (key.equals("prompt")) {
            results.put("Prompt", text);
        }
        else if (key.equals("uc")) {
            results.put("Negative", text);
        }

        // 2. Parameters
        else if (key.equals("scale")) {
            results.put("CFG", text);
        }
        else if (key.equals("steps")) {
            results.put("Steps", text);
        }
        else if (key.equals("seed")) {
            results.put("Seed", text);
        }
        else if (key.equals("sampler")) {
            results.put("Sampler", text);
        }

        // 3. Model Logic
        else if (key.equals("software") && text.equalsIgnoreCase("novelai")) {
            if (!results.containsKey("Model")) {
                results.put("Model", "NovelAI Diffusion");
            }
        }
    }
}