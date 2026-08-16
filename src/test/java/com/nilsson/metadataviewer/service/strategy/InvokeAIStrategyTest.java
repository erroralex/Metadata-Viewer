package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeAIStrategyTest {

    private final InvokeAIStrategy strategy = new InvokeAIStrategy();

    @Test
    void parse_extractsInvokeAiParams() {
        String json = """
                {
                    "app_version": "4.2.0",
                    "invokeai_metadata": {},
                    "model_name": "sd_xl_base_1.0",
                    "positive_prompt": "a forest clearing",
                    "negative_prompt": "blurry",
                    "cfg_scale": 7.5,
                    "sampler_name": "euler_a",
                    "scheduler": "karras"
                }
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("a forest clearing", result.get("Prompt"));
        assertEquals("euler_a", result.get("Sampler"));
    }
}
