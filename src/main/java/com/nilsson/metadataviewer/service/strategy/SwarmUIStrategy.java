package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class SwarmUIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isTextual()) return;
        String text = value.asText();

        // SwarmUI Model & Sampler
        if (key.equals("model") && text.length() > 4 && !text.contains("{")) {
            results.put("Model", text);
        }
        else if (key.equals("sampler")) {
            results.put("Sampler", text);
        }

        // SwarmUI Prompts
        else if (key.equals("prompt") && text.length() > 5) {
            if (!results.containsKey("Prompt")) {
                results.put("Prompt", text);
            }
        }
        else if (key.equals("negativeprompt")) {
            results.put("Negative", text);
        }
    }
}