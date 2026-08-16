package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwarmUIStrategyTest {

    private final SwarmUIStrategy strategy = new SwarmUIStrategy();

    @Test
    void parse_extractsSwarmUiParams() {
        String json = """
                {"sui_image_params": {
                    "model": "OfficialStableDiffusion/sd_xl_base_1.0",
                    "sampler": "euler",
                    "prompt": "a lighthouse at dusk",
                    "negativeprompt": "blurry",
                    "cfgscale": 7.0,
                    "steps": 20,
                    "seed": 42
                }}
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("euler", result.get("Sampler"));
        assertEquals("a lighthouse at dusk", result.get("Prompt"));
        assertEquals("20", result.get("Steps"));
        assertEquals("42", result.get("Seed"));
    }
}
