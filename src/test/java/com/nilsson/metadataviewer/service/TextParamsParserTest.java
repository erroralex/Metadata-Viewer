package com.nilsson.metadataviewer.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextParamsParserTest {

    private final TextParamsParser parser = new TextParamsParser();

    @Test
    void parse_detectsAndParsesA1111Text() {
        String text = "a beautiful sunset\nSteps: 20, Sampler: Euler a, CFG scale: 7, Seed: 123";

        Map<String, String> result = parser.parse(text);

        assertFalse(result.isEmpty());
        assertEquals("20", result.get("Steps"));
        assertEquals("Euler a", result.get("Sampler"));
    }

    @Test
    void parse_returnsEmptyMapForUnrecognizedText() {
        Map<String, String> result = parser.parse("just some random text");
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_returnsEmptyMapForNullOrBlank() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("   ").isEmpty());
    }
}
