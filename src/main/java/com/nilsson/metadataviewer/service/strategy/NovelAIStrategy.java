package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NovelAIStrategy implements MetadataStrategy {

    private static final ObjectMapper mapper = new ObjectMapper();

    public Map<String, String> parse(String text) {
        Map<String, String> results = new HashMap<>();
        try {
            JsonNode root = mapper.readTree(text);
            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                extract(field.getKey(), field.getValue(), root, results);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        // NovelAI sometimes uses numbers for config
        if (!value.isTextual() && !value.isNumber()) return;

        String text = value.asText().trim();
        if (text.isEmpty()) return;

        // 1. Prompts
        if (key.equals("prompt")) {
            // Prevent overwriting if a prompt was already found (e.g. by ComfyUI)
            if (!results.containsKey("Prompt")) {
                results.put("Prompt", text);
            }
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