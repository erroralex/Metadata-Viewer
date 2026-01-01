package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Iterator;
import java.util.Map;
import java.text.DecimalFormat;

public class ComfyUIStrategy implements MetadataStrategy {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {

        // 1. INPUTS BLOCK
        if (key.equalsIgnoreCase("inputs") && value.isObject()) {
            processInputsBlock(value, parentNode, results);
            return;
        }

        // 2. Standard Metadata
        if (!value.isTextual()) return;
        String text = value.asText();

        if (isComfyModelKey(key) && text.length() > 4 && !text.contains("{")) {
            updateModelField(results, text);
        } else if (key.startsWith("clip_name")) {
            updateModelField(results, "CLIP: " + text.replace(".safetensors", ""));
        } else if ((key.equals("vae_name") || key.equals("vae"))) {
            results.put("VAE", text);
        } else if (key.equals("sampler_name")) {
            String scheduler = parentNode.has("scheduler") ? parentNode.get("scheduler").asText() : "";
            results.put("Sampler", scheduler.isEmpty() ? text : text + " (" + scheduler + ")");
        } else if (key.equals("lora_name") || key.equals("lora")) {
            processLora(text, parentNode, results);
        }
    }

    private void processInputsBlock(JsonNode inputs, JsonNode node, Map<String, String> results) {
        Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String k = field.getKey();
            String kLower = k.toLowerCase().trim();
            JsonNode v = field.getValue();

            // --- NUMERIC FIELDS ---
            if (v.isNumber() || v.isIntegralNumber()) {
                String val = v.asText();
                double numVal = v.asDouble();

                // FIX: Ignore 0 or negative values (prevent overwriting real data with "0" from resize nodes)
                if (numVal <= 0) continue;

                if (kLower.equals("steps")) results.put("Steps", val);
                else if (kLower.equals("cfg") || kLower.equals("cfg_scale")) results.put("CFG", val);
                else if (kLower.contains("seed")) results.put("Seed", val);
                else if (kLower.equals("width")) results.put("Width", val);
                else if (kLower.equals("height")) results.put("Height", val);
                continue;
            }

            // --- TEXT FIELDS ---
            if (v.isTextual()) {
                String text = v.asText().trim();
                if (text.isEmpty()) continue;

                boolean shouldExtract = false;

                // Rule A: Explicit "prompt" key
                if (kLower.equals("prompt") || kLower.equals("captions")) {
                    shouldExtract = true;
                }
                // Rule B: Generic keys if node is Text/Prompt
                else if ((kLower.equals("text") || kLower.equals("text_g") || kLower.equals("text_l") || kLower.equals("string") || kLower.equals("value"))
                        && isTextOrPrimitiveNode(node)) {
                    shouldExtract = true;
                }

                if (shouldExtract) {
                    String target = isNegativeNode(node, inputs) ? "Negative" : "Prompt";
                    appendResult(results, target, text);
                }
            }
        }
    }

    private boolean isTextOrPrimitiveNode(JsonNode node) {
        if (node.has("class_type")) {
            String type = node.get("class_type").asText().toLowerCase();
            return type.contains("text") || type.contains("prompt") || type.contains("qwen") || type.contains("primitive");
        }
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            return title.contains("text") || title.contains("prompt") || title.contains("qwen");
        }
        return false;
    }

    private void appendResult(Map<String, String> results, String key, String newText) {
        String existing = results.get(key);
        if (existing == null || existing.isEmpty()) {
            results.put(key, newText);
        } else if (!existing.contains(newText)) {
            String sep = (existing.length() > 50 || newText.length() > 50) ? "\n\n" : ", ";
            results.put(key, existing + sep + newText);
        }
    }

    private boolean isNegativeNode(JsonNode node, JsonNode inputs) {
        if (node.has("_meta") && node.get("_meta").has("title")) {
            String title = node.get("_meta").get("title").asText().toLowerCase();
            if (title.contains("negative") || title.contains("neg ") || title.contains("(neg)")) return true;
        }
        if (node.has("class_type")) {
            String type = node.get("class_type").asText().toLowerCase();
            if (type.contains("negative")) return true;
        }
        return inputs.has("negative") || inputs.has("neg");
    }

    // --- Helpers ---
    private void processLora(String text, JsonNode parentNode, Map<String, String> results) {
        if (parentNode.has("on") && !parentNode.get("on").asBoolean()) return;

        double strModel = 1.0;
        double strClip = 1.0;
        boolean foundStrength = false;

        if (parentNode.has("strength_model")) { strModel = parentNode.get("strength_model").asDouble(); foundStrength = true; }
        if (parentNode.has("strength_clip")) { strClip = parentNode.get("strength_clip").asDouble(); foundStrength = true; }
        if (!foundStrength && parentNode.has("strength")) { strModel = parentNode.get("strength").asDouble(); strClip = strModel; }

        String cleanName = text.replace(".safetensors", "").replace(".pt", "");
        String finalEntry = cleanName;

        if (strModel != 1.0 || strClip != 1.0) {
            if (strModel == strClip) finalEntry += " (" + DF.format(strModel) + ")";
            else finalEntry += " (M:" + DF.format(strModel) + ", C:" + DF.format(strClip) + ")";
        }

        String existing = results.getOrDefault("Loras", "");
        if (existing.isEmpty() || existing.equals("None")) results.put("Loras", finalEntry);
        else if (!existing.contains(finalEntry)) results.put("Loras", existing + ", " + finalEntry);
    }

    private void updateModelField(Map<String, String> results, String newVal) {
        String existing = results.getOrDefault("Model", "");
        if (existing.isEmpty()) results.put("Model", newVal);
        else if (!existing.contains(newVal)) results.put("Model", existing + " + " + newVal);
    }

    private boolean isComfyModelKey(String key) {
        String k = key.toLowerCase();
        return k.equals("ckpt_name") || k.equals("unet_name") || k.equals("model_name") || k.contains("checkpoint");
    }
}