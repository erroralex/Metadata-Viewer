package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public class InvokeAIStrategy implements MetadataStrategy {

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        if (!value.isTextual()) return;
        String text = value.asText();

        // 1. Model Detection
        if (key.equals("model_name") || key.equals("model_weights")) {
            results.put("Model", text);
        }

        // 2. Prompts
        else if (key.equals("positive_prompt") || (key.equals("prompt") && !results.containsKey("Prompt"))) {
            results.put("Prompt", text);
        }
        else if (key.equals("negative_prompt")) {
            results.put("Negative", text);
        }

        // 3. Generation Params
        else if (key.equals("cfg_scale") || key.equals("cfg_rescale_multiplier")) {
            results.put("CFG", text);        }


        // This prevents overwriting "Sampler (Scheduler)" (from ComfyUI) with just "Sampler".
        else if ((key.equals("sampler_name") || key.equals("scheduler")) && !results.containsKey("Sampler")) {
            results.put("Sampler", text);
        }

        // 4. Variant Support
        else if (key.equals("variant") && !results.containsKey("Model")) {
            results.put("Model", text);
        }
    }
}