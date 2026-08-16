package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovelAIStrategyTest {

    private final NovelAIStrategy strategy = new NovelAIStrategy();

    @Test
    void parse_extractsNovelAiParams() {
        String json = """
                {
                    "Software": "NovelAI",
                    "prompt": "a shrine in the rain",
                    "uc": "blurry, watermark",
                    "scale": 5.0,
                    "steps": 28,
                    "seed": 555,
                    "sampler": "k_euler_ancestral"
                }
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("a shrine in the rain", result.get("Prompt"));
        assertEquals("28", result.get("Steps"));
        assertEquals("555", result.get("Seed"));
    }
}
