package com.nilsson.metadataviewer.service.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * specialized parser for A1111, Forge, and Fooocus text blocks.
 */
public class TextParamsParser {

    public void parse(String raw, Map<String, String> results) {
        String[] sections = raw.split("\nSteps:");
        String promptPart = sections[0];

        // 1. Prompts
        if (promptPart.contains("Negative prompt:")) {
            String[] split = promptPart.split("Negative prompt:");
            results.put("Prompt", split[0].trim());
            results.put("Negative", split[1].trim());
        } else {
            results.put("Prompt", promptPart.trim());
        }

        // 2. Parameters Footer
        if (sections.length > 1) {
            String footer = "Steps:" + sections[1];
            results.put("Model", extractRegex(footer, "Model: ([^,]+)"));
            results.put("Steps", extractRegex(footer, "Steps: ([^,]+)"));
            results.put("CFG", extractRegex(footer, "CFG scale: ([^,]+)"));
            results.put("Seed", extractRegex(footer, "Seed: ([^,]+)"));

            results.put("Size", extractRegex(footer, "Size: ([^,]+)"));
            String sampler = extractRegex(footer, "Sampler: ([^,]+)");
            String scheduler = extractRegex(footer, "Schedule type: ([^,]+)");
            results.put("Sampler", (scheduler.equals("N/A") || scheduler.isEmpty()) ? sampler : sampler + " (" + scheduler + ")");

            String vae = extractRegex(footer, "VAE: ([^,]+)");
            if (!vae.equals("N/A")) results.put("VAE", vae);

            // Hires Fix
            String hiresUpscale = extractRegex(footer, "Hires upscale: ([^,]+)");
            if (!hiresUpscale.equals("N/A")) {
                String hiresRes = extractRegex(footer, "Hires resize: ([^,]+)");
                results.put("Hires. fix", hiresUpscale + "x (" + hiresRes + ")");
            }

            // Loras
            Pattern p = Pattern.compile("<lora:([^:]+):");
            Matcher m = p.matcher(raw);
            Set<String> loras = new LinkedHashSet<>();
            while (m.find()) loras.add(m.group(1));
            results.put("Loras", loras.isEmpty() ? "None" : String.join(", ", loras));
        }
    }

    private String extractRegex(String src, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(src);
        return m.find() ? m.group(1).trim() : "N/A";
    }
}