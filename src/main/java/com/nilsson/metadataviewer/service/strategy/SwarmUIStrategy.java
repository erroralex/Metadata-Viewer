package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SwarmUIStrategy implements MetadataStrategy {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Entry point called by TextParamsParser
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
        if (!value.isTextual() && !value.isNumber()) return;
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

        // SwarmUI Parameters
        else if (key.equals("cfgscale")) {
            results.put("CFG", text);
        }
        else if (key.equals("steps")) {
            results.put("Steps", text);
        }
        else if (key.equals("seed")) {
            results.put("Seed", text);
        }
    }
}