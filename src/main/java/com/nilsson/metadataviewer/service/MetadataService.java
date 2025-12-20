package com.nilsson.metadataviewer.service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for extracting AI Generation metadata from images.
 * Supports A1111, Forge, Reforge, ComfyUI, and SwarmUI formats.
 */
public class MetadataService {

    /**
     * Extracts raw metadata string and parses it into a structured Map.
     */
    public Map<String, String> getExtractedData(File file) {
        Map<String, String> results = new HashMap<>();
        String rawData = extractRawMetadata(file);

        if (rawData == null || rawData.isEmpty()) {
            results.put("Prompt", "No metadata found in this image.");
            return results;
        }

        results.put("Raw", rawData);

        // A1111 / Forge Style Parsing
        if (rawData.contains("Steps:") && rawData.contains("Sampler:")) {
            parseA1111Format(rawData, results);
        }
        // ComfyUI / JSON Style Parsing (Basic Detection)
        else if (rawData.trim().startsWith("{")) {
            results.put("Prompt", "ComfyUI/JSON workflow detected. Raw data displayed below.");
            results.put("Type", "ComfyUI");
        } else {
            results.put("Prompt", rawData);
        }

        return results;
    }

    private String extractRawMetadata(File file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return null;

            ImageReader reader = readers.next();
            reader.setInput(input);
            IIOMetadata metadata = reader.getImageMetadata(0);
            Node root = metadata.getAsTree("javax_imageio_png_1.0");

            return findChunkValue(root);
        } catch (Exception e) {
            return null;
        }
    }

    private String findChunkValue(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child.getNodeName().equals("tEXt") || child.getNodeName().equals("iTXt")) {
                NamedNodeMap attr = child.getFirstChild().getAttributes();
                String keyword = attr.getNamedItem("keyword").getNodeValue();
                String value = attr.getNamedItem("value").getNodeValue();

                if ("parameters".equalsIgnoreCase(keyword) || "prompt".equalsIgnoreCase(keyword)) {
                    return value;
                }
            }
            String nested = findChunkValue(child);
            if (nested != null) return nested;
            child = child.getNextSibling();
        }
        return null;
    }

    private void parseA1111Format(String raw, Map<String, String> results) {
        // Split by the common footer marker in A1111
        String[] parts = raw.split("\nSteps:");
        results.put("Prompt", parts[0].trim());

        if (parts.length > 1) {
            String footer = "Steps:" + parts[1];
            results.put("Model", extractRegex(footer, "Model: ([^,]+)"));
            results.put("Steps", extractRegex(footer, "Steps: ([^,]+)"));
            results.put("Sampler", extractRegex(footer, "Sampler: ([^,]+)"));
            results.put("CFG", extractRegex(footer, "CFG scale: ([^,]+)"));
            results.put("Seed", extractRegex(footer, "Seed: ([^,]+)"));

            // Lora Extraction
            Pattern loraPattern = Pattern.compile("<lora:([^:]+):[^>]+>");
            Matcher matcher = loraPattern.matcher(raw);
            StringBuilder loras = new StringBuilder();
            while (matcher.find()) {
                if (loras.length() > 0) loras.append(", ");
                loras.append(matcher.group(1));
            }
            results.put("Loras", loras.length() > 0 ? loras.toString() : "None");
        }
    }

    private String extractRegex(String source, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(source);
        return m.find() ? m.group(1).trim() : "N/A";
    }
}