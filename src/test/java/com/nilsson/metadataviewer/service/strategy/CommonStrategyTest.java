package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommonStrategyTest {

    private final CommonStrategy strategy = new CommonStrategy();

    @Test
    void parse_extractsA1111Params() {
        String text = "a photo of a cat\nNegative prompt: blurry, low quality\n"
                + "Steps: 24, Sampler: DPM++ 2M Karras, CFG scale: 6.5, Seed: 987654321, "
                + "Size: 768x1152, Model: sd_xl_base_1.0, Model hash: 31e35c80fc, "
                + "Denoising strength: 0.4, Hires upscale: 2";

        Map<String, String> result = strategy.parse(text);

        assertEquals("24", result.get("Steps"));
        assertEquals("DPM++ 2M Karras", result.get("Sampler"));
        assertEquals("6.5", result.get("CFG"));
        assertEquals("987654321", result.get("Seed"));
        assertEquals("sd_xl_base_1.0", result.get("Model"));
        assertTrue(result.get("Prompt").contains("a photo of a cat"));
        assertTrue(result.get("Negative").contains("blurry"));
    }
}
