package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComfyUIStrategy implements MetadataStrategy {

    // Ensure dot separator for decimals regardless of locale
    private static final DecimalFormat DF = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    private static final Set<String> VALID_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".safetensors", ".ckpt", ".gguf", ".pt", ".pth", ".bin"
    ));

    private static final Set<String> IGNORED_FILENAME_PATTERNS = new HashSet<>(Arrays.asList(
            "upscale", "esrgan", "controlnet", "ipadapter", "faceid", "adapter",
            "clip", "vae", "preview", "t5", "encoder", "refiner",
            "bbox", "yolo", "ultralytics", "mediapipe", "segs", "detailer", "mask", "inpaint"
    ));

    private static final Set<String> ALLOWED_MODEL_NODE_TYPES = new HashSet<>(Arrays.asList(
            "checkpoint", "unet", "loader", "lora"
    ));

    private static final Set<String> IGNORED_MODEL_NODE_TYPES = new HashSet<>(Arrays.asList(
            "preprocessor", "detailer", "output", "save image", "preview image", "save", "preview", "detector", "mask"
    ));

    private static final Set<String> SAMPLER_KEYWORDS = new HashSet<>(Arrays.asList(
            "euler", "heun", "dpm", "lms", "ddim", "uni_pc", "lcm", "multistep", "singlestep"
    ));

    private static final Set<String> SCHEDULER_KEYWORDS = new HashSet<>(Arrays.asList(
            "normal", "karras", "exponential", "sgm", "simple", "beta", "ddim", "standard", "linear", "uniform", "gpu", "polyexponential", "automatic"
    ));

    private static final Pattern LORA_TAG_PATTERN = Pattern.compile("<lora:([^:>]+)(?::([^:>]+))?.*?>");

    @Override
    public void extract(String key, JsonNode value, JsonNode parentNode, Map<String, String> results) {
        try {
            if (key.equalsIgnoreCase("nodes") && value.isArray()) {
                // UI Format processing
                processNodes(value, parentNode.get("links"), results);
            }
            else if (key.equals("api_nodes") && value.isObject()) {
                // API Format processing (Wrapped)
                processApiWorkflow(value, results);
                results.put("_api_graph_analyzed", "true");
            }
            // Detect Raw API Format (Node ID keys at root)
            else if (isNodeId(key) && value.has("class_type") && value.has("inputs")) {
                if (!results.containsKey("_api_graph_analyzed")) {
                    processApiWorkflow(parentNode, results);
                    results.put("_api_graph_analyzed", "true");
                }
            }
            else if (key.equalsIgnoreCase("inputs") && value.isObject()) {
                boolean skipCoreParams = results.containsKey("_api_graph_analyzed");
                processInputsBlock(value, parentNode, results, skipCoreParams);
            }
            else if (key.equalsIgnoreCase("extra") && value.has("seed_widgets") && !results.containsKey("_seed_locked")) {
                processGlobalSeedMap(value.get("seed_widgets"), parentNode.get("nodes"), results);
            }
        } catch (Exception e) {}
    }

    private boolean isNodeId(String key) {
        return key.matches("\\d+");
    }

    // --- API FORMAT HANDLING ---
    private void processApiWorkflow(JsonNode root, Map<String, String> results) {
        JsonNode bestSampler = null;
        long maxSteps = -1;
        double fluxGuidance = -1;
        String directSchedulerFound = null;

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();
            String type = getNodeType(node).toLowerCase();

            // 1. Extract globals from every node (Prompts, Models, etc.)
            if (node.has("inputs")) {
                processInputsBlock(node.get("inputs"), node, results, false);
                double g = resolveFloatParamRecursive(node, "guidance", root);
                if (g > -1) fluxGuidance = g;

                // SPECIAL HANDLER: Power Lora Loader
                if (type.contains("power lora loader")) {
                    extractPowerLoras(node.get("inputs"), results);
                }
            }

            // 2. Find Best Sampler & Detect Scheduler Nodes
            if (type.contains("scheduler") && node.has("inputs")) {
                JsonNode schedVal = node.get("inputs").get("scheduler");
                if (schedVal != null && schedVal.isTextual()) {
                    directSchedulerFound = schedVal.asText();
                }
            }

            long steps = -1;
            if (type.contains("samplercustom")) {
                JsonNode sigmasNode = getLinkedNodeApi(node, "sigmas", root);
                if (sigmasNode != null) steps = resolveNumericParamRecursive(sigmasNode, "steps", root);
            } else if (type.contains("sampler") && !type.contains("detailer") && !type.contains("upscale")) {
                steps = resolveNumericParamRecursive(node, "steps", root);
            }

            if (steps > maxSteps) {
                maxSteps = steps;
                bestSampler = node;
            }
        }

        // 3. Extract Core Params from Best Sampler
        if (bestSampler != null) {
            results.put("Steps", String.valueOf(maxSteps));

            long s = resolveNumericParamRecursive(bestSampler, "seed", root);
            if (s == -1) s = resolveNumericParamRecursive(bestSampler, "noise_seed", root);
            if (s == -1) {
                JsonNode noiseNode = getLinkedNodeApi(bestSampler, "noise", root);
                if (noiseNode != null) s = resolveNumericParamRecursive(noiseNode, "noise_seed", root);
            }
            if (s > -1) results.put("Seed", String.valueOf(s));

            double cfg = resolveFloatParamRecursive(bestSampler, "cfg", root);
            if (cfg == -1) {
                JsonNode guiderNode = getLinkedNodeApi(bestSampler, "guider", root);
                if (guiderNode != null) cfg = resolveFloatParamRecursive(guiderNode, "cfg", root);
            }

            if (cfg > -1) {
                String cfgStr = DF.format(cfg);
                if (fluxGuidance > -1) {
                    cfgStr += " (distilled " + DF.format(fluxGuidance) + ")";
                }
                results.put("CFG", cfgStr);
            }

            String samp = resolveStringParamRecursive(bestSampler, "sampler_name", root);
            if (samp == null) {
                JsonNode sampNode = getLinkedNodeApi(bestSampler, "sampler", root);
                if (sampNode != null) samp = resolveStringParamRecursive(sampNode, "sampler_name", root);
            }

            String sched = resolveStringParamRecursive(bestSampler, "scheduler", root);
            if (sched == null) {
                JsonNode sigNode = getLinkedNodeApi(bestSampler, "sigmas", root);
                if (sigNode != null) sched = resolveStringParamRecursive(sigNode, "scheduler", root);
            }

            if (sched == null && directSchedulerFound != null) sched = directSchedulerFound;
            if (sched == null && results.containsKey("Scheduler")) sched = results.get("Scheduler");

            if (samp != null) {
                if (sched != null) {
                    results.put("Sampler", samp + " (" + sched + ")");
                } else {
                    results.put("Sampler", samp);
                }
            } else if (sched != null) {
                results.put("Sampler", sched);
            }
        }
    }

    // --- UI FORMAT PROCESSING ---
    private void processNodes(JsonNode nodes, JsonNode links, Map<String, String> results) {
        Map<Integer, Integer> linkMap = buildLinkMap(links);
        Map<Integer, JsonNode> nodeMap = buildNodeMap(nodes);
        Map<Integer, List<JsonNode>> linkDestMap = buildLinkDestMap(nodes);

        JsonNode bestSampler = null;
        long maxSteps = -1;
        double fluxGuidance = -1;
        boolean userSeedFound = false;

        for (JsonNode node : nodes) {
            String type = getNodeType(node).toLowerCase();
            String title = getNodeTitle(node).toLowerCase();
            JsonNode widgets = node.get("widgets_values");

            // 1. SEED
            if (type.contains("primitive") && (title.contains("seed") || title.contains("noise"))) {
                long val = extractFirstNumeric(widgets);
                if (val > -1) {
                    results.put("Seed", String.valueOf(val));
                    results.put("_seed_locked", "true");
                    userSeedFound = true;
                }
            }

            // 2. MAIN SAMPLER
            if (type.contains("sampler") && !type.contains("detailer") && !type.contains("upscale")) {
                long steps = resolveNumericParam(node, "steps", nodeMap, linkMap);
                if (steps == -1) steps = extractStepsFromWidgets(widgets);

                if (steps > maxSteps) {
                    maxSteps = steps;
                    bestSampler = node;
                }
            }

            // 3. FLUX GUIDANCE
            if (title.contains("guidance") || type.contains("guider")) {
                double g = resolveFloatParam(node, "guidance", nodeMap, linkMap);
                if (g > -1) fluxGuidance = g;
            }

            // 4. MODELS
            if (isAllowedModelNode(type)) {
                if (type.contains("loraloader")) extractLoras(widgets, results);
                else extractModelFromList(widgets, results);
            }

            // SPECIAL HANDLER: Power Lora Loader (UI Format)
            if (type.contains("power lora loader") && node.has("inputs")) {
                extractPowerLoras(node.get("inputs"), results);
            }

            // 5. PROMPTS
            if (isTextOrPrimitiveNode(node) && widgets != null) {
                if (!hasOutputs(node) && !type.contains("showtext") && !type.contains("note")) {
                    continue;
                }

                String role = detectTextRole(node, linkDestMap);
                if ("Disconnected".equals(role)) continue;

                if ("Unknown".equals(role) || "Prompt".equals(role)) {
                    if (title.contains("negative") || isNegativeNode(node)) {
                        role = "Negative";
                    } else {
                        role = "Prompt";
                    }
                }

                extractPromptText(node, widgets, results, role);
            }
        }

        // EXTRACT FROM MAIN SAMPLER
        if (bestSampler != null) {
            if (maxSteps > -1) results.put("Steps", String.valueOf(maxSteps));

            if (!userSeedFound) {
                long s = resolveNumericParam(bestSampler, "seed", nodeMap, linkMap);
                if (s == -1) s = resolveNumericParam(bestSampler, "noise_seed", nodeMap, linkMap);
                if (s == -1) s = extractSeedFromWidgets(bestSampler.get("widgets_values"));
                if (s > -1 && !results.containsKey("Seed")) results.put("Seed", String.valueOf(s));
            }

            double cfg = resolveFloatParam(bestSampler, "cfg", nodeMap, linkMap);
            if (cfg == -1) {
                JsonNode guiderNode = getLinkedNode(bestSampler, "guider", nodeMap, linkMap);
                if (guiderNode != null) {
                    cfg = resolveFloatParam(guiderNode, "cfg", nodeMap, linkMap);
                }
            }
            if (cfg == -1) cfg = extractCfgFromWidgets(bestSampler.get("widgets_values"));

            if (cfg > -1) {
                String cfgStr = DF.format(cfg);
                if (fluxGuidance > -1) {
                    cfgStr += " (distilled " + DF.format(fluxGuidance) + ")";
                }
                results.put("CFG", cfgStr);
            }

            String samp = resolveStringParam(bestSampler, "sampler", nodeMap, linkMap);
            if (samp == null) {
                JsonNode sampNode = getLinkedNode(bestSampler, "sampler", nodeMap, linkMap);
                if (sampNode != null && sampNode.has("inputs") && sampNode.get("inputs").has("sampler_name")) {
                    samp = sampNode.get("inputs").get("sampler_name").asText();
                }
            }
            if (samp == null) samp = extractKeyword(bestSampler.get("widgets_values"), SAMPLER_KEYWORDS);

            String sched = resolveStringParam(bestSampler, "scheduler", nodeMap, linkMap);
            if (sched == null) {
                JsonNode sigNode = getLinkedNode(bestSampler, "sigmas", nodeMap, linkMap);
                if (sigNode != null && sigNode.has("inputs") && sigNode.get("inputs").has("scheduler")) {
                    sched = sigNode.get("inputs").get("scheduler").asText();
                }
            }
            if (sched == null) sched = extractKeyword(bestSampler.get("widgets_values"), SCHEDULER_KEYWORDS);

            if (samp != null) {
                if (sched != null) {
                    results.put("Sampler", samp + " (" + sched + ")");
                } else {
                    results.put("Sampler", samp);
                }
            } else if (sched != null) {
                results.put("Sampler", sched);
            }
        }
    }

    // --- HELPER: Unified Lora Formatter ---
    private String formatLoraString(String name, double strength) {
        // Format: <lora:name:strength>
        return "<lora:" + name + ":" + DF.format(strength) + ">";
    }

    private void extractPowerLoras(JsonNode inputs, Map<String, String> results) {
        if (inputs == null) return;
        Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode val = entry.getValue();

            if (key.startsWith("lora") && val.isObject()) {
                boolean isOn = val.has("on") && val.get("on").asBoolean(false);
                if (isOn && val.has("lora")) {
                    String name = val.get("lora").asText();
                    double strength = val.has("strength") ? val.get("strength").asDouble(1.0) : 1.0;

                    if (isValidModelFile(name)) {
                        name = cleanFilename(name);
                        appendResult(results, "Loras", formatLoraString(name, strength));
                    }
                }
            }
        }
    }

    private void extractLoras(JsonNode widgets, Map<String, String> results) {
        if (widgets == null) return;
        String name = null;
        double strength = 1.0;
        for (JsonNode w : widgets) {
            if (w.isTextual() && isValidModelFile(w.asText())) name = cleanFilename(w.asText());
            else if (isNumeric(w)) strength = w.asDouble();
        }
        if (name != null) {
            String existing = results.getOrDefault("Loras", "");
            if (!existing.contains(name)) {
                appendResult(results, "Loras", formatLoraString(name, strength));
            }
        }
    }

    private void extractLorasFromPrompt(String promptText, Map<String, String> results) {
        if (promptText == null || !promptText.contains("<lora:")) return;
        Matcher m = LORA_TAG_PATTERN.matcher(promptText);
        while (m.find()) {
            String name = cleanFilename(m.group(1));
            String strVal = m.group(2);
            double strength = 1.0;
            if (strVal != null) { try { strength = Double.parseDouble(strVal); } catch (NumberFormatException e) {} }

            String existing = results.getOrDefault("Loras", "");
            if (!existing.contains(name)) {
                appendResult(results, "Loras", formatLoraString(name, strength));
            }
        }
    }

    private String detectTextRole(JsonNode node, Map<Integer, List<JsonNode>> linkDestMap) {
        if (!node.has("outputs")) return "Unknown";
        return traceNodeOutputs(node, linkDestMap, 0);
    }

    private String traceNodeOutputs(JsonNode node, Map<Integer, List<JsonNode>> linkDestMap, int depth) {
        if (depth > 12) return "Unknown";
        boolean hasAnyValidPath = false;
        if (node.has("outputs")) {
            for (JsonNode output : node.get("outputs")) {
                if (output.has("links")) {
                    for (JsonNode link : output.get("links")) {
                        int linkId = link.asInt();
                        List<JsonNode> destinations = linkDestMap.get(linkId);
                        if (destinations != null) {
                            for (JsonNode dest : destinations) {
                                if (isLinkBlocked(dest, linkId)) continue;
                                hasAnyValidPath = true;
                                String destType = getNodeType(dest);
                                if (destType.contains("sampler")) {
                                    if (dest.has("inputs")) {
                                        for (JsonNode input : dest.get("inputs")) {
                                            if (input.has("link") && input.get("link").asInt() == linkId) {
                                                String inputName = input.get("name").asText().toLowerCase();
                                                if (inputName.contains("negative")) return "Negative";
                                                if (inputName.contains("positive")) return "Prompt";
                                            }
                                        }
                                    }
                                }
                                if (destType.contains("reroute") || destType.contains("pipe") || destType.contains("bus")
                                        || destType.contains("switch") || destType.contains("concatenate")
                                        || destType.contains("replace") || destType.contains("processor")
                                        || destType.contains("string") || destType.contains("text")) {
                                    String res = traceNodeOutputs(dest, linkDestMap, depth + 1);
                                    if ("Negative".equals(res) || "Positive".equals(res) || "Prompt".equals(res)) return res;
                                }
                            }
                        }
                    }
                }
            }
        }
        return hasAnyValidPath ? "Unknown" : "Disconnected";
    }

    private boolean isLinkBlocked(JsonNode destNode, int incomingLinkId) {
        String type = getNodeType(destNode);
        if (type.contains("jps_dynamicpromptconcatenate")) {
            if (destNode.has("inputs") && destNode.has("widgets_values")) {
                JsonNode inputs = destNode.get("inputs");
                JsonNode widgets = destNode.get("widgets_values");
                for (JsonNode input : inputs) {
                    if (input.has("link") && input.get("link").asInt() == incomingLinkId) {
                        String name = input.get("name").asText();
                        if (name.startsWith("text_")) {
                            try {
                                int index = Integer.parseInt(name.replace("text_", ""));
                                if (index < widgets.size()) {
                                    JsonNode toggle = widgets.get(index);
                                    if (toggle.isBoolean() && !toggle.asBoolean()) return true;
                                }
                            } catch (NumberFormatException e) {}
                        }
                    }
                }
            }
        }
        return false;
    }

    private JsonNode getLinkedNodeApi(JsonNode node, String inputName, JsonNode root) {
        if (!node.has("inputs")) return null;
        JsonNode inputs = node.get("inputs");
        if (inputs.has(inputName)) {
            JsonNode val = inputs.get(inputName);
            if (val.isArray() && val.size() == 2) return root.get(val.get(0).asText());
        }
        return null;
    }

    private long resolveNumericParamRecursive(JsonNode node, String paramName, JsonNode root) {
        if (!node.has("inputs")) return -1;
        JsonNode inputs = node.get("inputs");
        if (inputs.has(paramName)) {
            JsonNode val = inputs.get(paramName);
            if (isNumeric(val)) return asLongSafe(val);
            if (val.isArray() && val.size() == 2) return resolveValueRecursive(root.get(val.get(0).asText()), root);
        }
        return -1;
    }

    private double resolveFloatParamRecursive(JsonNode node, String paramName, JsonNode root) {
        if (!node.has("inputs")) return -1;
        JsonNode inputs = node.get("inputs");
        if (inputs.has(paramName)) {
            JsonNode val = inputs.get(paramName);
            if (isNumeric(val)) return val.asDouble();
            if (val.isArray() && val.size() == 2) return resolveFloatValueRecursive(root.get(val.get(0).asText()), root);
        }
        return -1;
    }

    private String resolveStringParamRecursive(JsonNode node, String paramName, JsonNode root) {
        if (!node.has("inputs")) return null;
        JsonNode inputs = node.get("inputs");
        if (inputs.has(paramName)) {
            JsonNode val = inputs.get(paramName);
            if (val.isTextual()) return val.asText();
            if (val.isArray() && val.size() == 2) {
                JsonNode source = root.get(val.get(0).asText());
                if (source != null && source.has("inputs")) {
                    JsonNode srcIn = source.get("inputs");
                    if (srcIn.has("Value")) return srcIn.get("Value").asText();
                    if (srcIn.has("text")) return srcIn.get("text").asText();
                    if (srcIn.has("string")) return srcIn.get("string").asText();
                }
            }
        }
        return null;
    }

    private long resolveValueRecursive(JsonNode node, JsonNode root) {
        if (node == null || !node.has("inputs")) return -1;
        JsonNode inputs = node.get("inputs");
        if (inputs.has("Value")) return asLongSafe(inputs.get("Value"));
        if (inputs.has("value")) return asLongSafe(inputs.get("value"));
        if (inputs.has("seed")) return asLongSafe(inputs.get("seed"));
        return -1;
    }

    private double resolveFloatValueRecursive(JsonNode node, JsonNode root) {
        if (node == null || !node.has("inputs")) return -1;
        JsonNode inputs = node.get("inputs");
        if (inputs.has("Value")) return inputs.get("Value").asDouble();
        if (inputs.has("value")) return inputs.get("value").asDouble();
        return -1;
    }

    private JsonNode getLinkedNode(JsonNode sourceNode, String inputName, Map<Integer, JsonNode> nodeMap, Map<Integer, Integer> linkMap) {
        if (!sourceNode.has("inputs")) return null;
        for (JsonNode input : sourceNode.get("inputs")) {
            String name = input.get("name").asText().toLowerCase();
            if (name.equals(inputName.toLowerCase()) && input.has("link") && input.get("link").isInt()) {
                int linkId = input.get("link").asInt();
                if (linkMap.containsKey(linkId)) return nodeMap.get(linkMap.get(linkId));
            }
        }
        return null;
    }

    private boolean isAllowedModelNode(String type) {
        if (IGNORED_MODEL_NODE_TYPES.stream().anyMatch(type::contains)) return false;
        return ALLOWED_MODEL_NODE_TYPES.stream().anyMatch(type::contains);
    }

    private void processGlobalSeedMap(JsonNode seedWidgets, JsonNode nodes, Map<String, String> results) {
        Iterator<Map.Entry<String, JsonNode>> fields = seedWidgets.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String targetNodeId = entry.getKey();
            int widgetIndex = entry.getValue().asInt();
            for (JsonNode node : nodes) {
                if (String.valueOf(node.get("id").asInt()).equals(targetNodeId)) {
                    JsonNode widgets = node.get("widgets_values");
                    if (widgets != null && widgets.size() > widgetIndex) {
                        JsonNode val = widgets.get(widgetIndex);
                        if (isNumeric(val)) results.put("Seed", val.asText());
                    }
                }
            }
        }
    }

    private void processInputsBlock(JsonNode inputs, JsonNode node, Map<String, String> results, boolean skipCoreParams) {
        String type = getNodeType(node).toLowerCase();
        String title = getNodeTitle(node).toLowerCase();
        Iterator<Map.Entry<String, JsonNode>> fields = inputs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String k = field.getKey().toLowerCase();
            JsonNode v = field.getValue();

            if (!skipCoreParams) {
                if (k.equals("scheduler")) results.put("Scheduler", v.asText());
                if (k.equals("sampler_name")) results.put("Sampler", v.asText());
            }

            if (isNumeric(v)) {
                if (!skipCoreParams) {
                    if (k.equals("steps")) results.put("Steps", v.asText());
                    if (k.equals("cfg") || k.equals("cfg_scale")) results.put("CFG", v.asText());
                    if ((k.equals("seed") || k.equals("noise_seed")) && !results.containsKey("_seed_locked")) results.put("Seed", v.asText());
                }
            } else if (v.isTextual()) {
                String txt = v.asText();
                if (isAllowedModelNode(type) && isValidModelFile(txt) && (k.contains("ckpt") || k.contains("model") || k.contains("unet") || k.contains("file"))) {
                    results.put("Model", cleanFilename(txt));
                }
                if (isValidPrompt(txt) && (k.contains("prompt") || k.contains("text"))) {
                    String targetKey = (title.contains("negative") || isNegativeNode(node)) ? "Negative" : "Prompt";
                    appendResult(results, targetKey, txt);
                    extractLorasFromPrompt(txt, results);
                }
            }
        }
    }

    private long extractFirstNumeric(JsonNode widgets) {
        if (widgets == null) return -1;
        for (JsonNode w : widgets) if (isNumeric(w)) return asLongSafe(w);
        return -1;
    }

    private Map<Integer, Integer> buildLinkMap(JsonNode links) {
        Map<Integer, Integer> map = new HashMap<>();
        if (links != null) for (JsonNode link : links) if (link.size() >= 2) map.put(link.get(0).asInt(), link.get(1).asInt());
        return map;
    }

    private Map<Integer, List<JsonNode>> buildLinkDestMap(JsonNode nodes) {
        Map<Integer, List<JsonNode>> map = new HashMap<>();
        for (JsonNode node : nodes) {
            if (node.has("inputs")) {
                for (JsonNode input : node.get("inputs")) {
                    if (input.has("link") && input.get("link").isInt()) {
                        int linkId = input.get("link").asInt();
                        map.computeIfAbsent(linkId, k -> new ArrayList<>()).add(node);
                    }
                }
            }
        }
        return map;
    }

    private Map<Integer, JsonNode> buildNodeMap(JsonNode nodes) {
        Map<Integer, JsonNode> map = new HashMap<>();
        for (JsonNode node : nodes) map.put(node.get("id").asInt(), node);
        return map;
    }

    private boolean isNumeric(JsonNode node) {
        if (node == null || node.isBoolean()) return false;
        return node.isNumber() || (node.isTextual() && node.asText().matches("-?\\d+(\\.\\d+)?"));
    }

    private boolean isInteger(JsonNode node) {
        if (!isNumeric(node)) return false;
        String t = node.asText();
        return !t.contains(".") || t.endsWith(".0");
    }

    private long asLongSafe(JsonNode node) {
        try { return node.isNumber() ? node.asLong() : Long.parseLong(node.asText().split("\\.")[0]); } catch (Exception e) { return -1; }
    }

    private boolean isValidModelFile(String filename) {
        if (filename == null || filename.length() < 3) return false;
        String lower = filename.toLowerCase();
        if (VALID_EXTENSIONS.stream().noneMatch(lower::endsWith)) return false;
        if (IGNORED_FILENAME_PATTERNS.stream().anyMatch(lower::contains)) return false;
        return !lower.equals("true") && !lower.equals("false") && !lower.equals("none");
    }

    private String cleanFilename(String path) {
        if (path.contains("\\")) path = path.substring(path.lastIndexOf("\\") + 1);
        if (path.contains("/")) path = path.substring(path.lastIndexOf("/") + 1);
        return path.replaceAll("\\.(safetensors|gguf|ckpt|pt|pth|bin)$", "");
    }

    private boolean isValidPrompt(String text) {
        if (text.length() < 5) return false;
        if (isValidModelFile(text)) return false;
        if (text.startsWith("comma") || text.startsWith("newline")) return false;
        return !text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false");
    }

    private String getNodeType(JsonNode node) {
        if (node.has("class_type")) return node.get("class_type").asText().toLowerCase();
        if (node.has("type")) return node.get("type").asText().toLowerCase();
        return "";
    }

    private String getNodeTitle(JsonNode node) {
        if (node.has("title")) return node.get("title").asText().toLowerCase();
        if (node.has("_meta") && node.get("_meta").has("title")) return node.get("_meta").get("title").asText().toLowerCase();
        return "";
    }

    private boolean isTextOrPrimitiveNode(JsonNode node) {
        String type = getNodeType(node);
        return type.contains("text") || type.contains("prompt") || type.contains("primitive") || type.contains("string") ||
                type.contains("portrait") || type.contains("processor") || type.contains("wildcard") ||
                type.contains("manager") || type.contains("janus");
    }

    private boolean isNegativeNode(JsonNode node) {
        String title = getNodeTitle(node).toLowerCase();
        if (title.contains("negative")) return true;
        String type = getNodeType(node);
        return type.contains("negative") || type.contains("neg ");
    }

    private boolean hasOutputs(JsonNode node) {
        if (!node.has("outputs")) return false;
        for (JsonNode output : node.get("outputs")) {
            if (output.has("links")) {
                JsonNode links = output.get("links");
                if (links.isArray() && links.size() > 0) return true;
            }
        }
        return false;
    }

    private void appendResult(Map<String, String> results, String key, String newText) {
        String existing = results.get(key);
        if (existing == null || existing.isEmpty()) results.put(key, newText);
        else if (!existing.contains(newText)) results.put(key, existing + ", " + newText);
    }

    private long resolveNumericParam(JsonNode node, String paramName, Map<Integer, JsonNode> nodeMap, Map<Integer, Integer> linkMap) {
        if (!node.has("inputs")) return -1;
        for (JsonNode input : node.get("inputs")) {
            String name = input.get("name").asText().toLowerCase();
            if (name.contains(paramName)) {
                if (input.has("link") && input.get("link").isInt()) {
                    int linkId = input.get("link").asInt();
                    if (linkMap.containsKey(linkId)) {
                        int sourceId = linkMap.get(linkId);
                        JsonNode source = nodeMap.get(sourceId);
                        if (source != null) {
                            long val = extractFirstNumeric(source.get("widgets_values"));
                            if (val > -1) return val;
                        }
                    }
                }
                if (input.has("widget") && input.get("widget").has("value")) {
                    JsonNode val = input.get("widget").get("value");
                    if (isNumeric(val)) return val.asLong();
                }
            }
        }
        return -1;
    }

    private double resolveFloatParam(JsonNode node, String paramName, Map<Integer, JsonNode> nodeMap, Map<Integer, Integer> linkMap) {
        if (node.has("inputs")) {
            for (JsonNode input : node.get("inputs")) {
                String name = input.get("name").asText().toLowerCase();
                if (name.contains(paramName)) {
                    if (input.has("link") && input.get("link").isInt()) {
                        int linkId = input.get("link").asInt();
                        if (linkMap.containsKey(linkId)) {
                            int sourceId = linkMap.get(linkId);
                            JsonNode source = nodeMap.get(sourceId);
                            if (source != null) {
                                if (source.has("widgets_values")) {
                                    for (JsonNode w : source.get("widgets_values")) if (isNumeric(w)) return w.asDouble();
                                }
                                if (source.has("inputs") && source.get("inputs").has(paramName)) {
                                    JsonNode v = source.get("inputs").get(paramName);
                                    if (isNumeric(v)) return v.asDouble();
                                }
                            }
                        }
                    }
                    if (input.has("widget") && input.get("widget").has("value")) {
                        JsonNode val = input.get("widget").get("value");
                        if (isNumeric(val)) return val.asDouble();
                    }
                }
            }
        }
        if (node.has("widgets_values")) {
            for (JsonNode w : node.get("widgets_values")) {
                if (isNumeric(w) && !w.isBoolean()) {
                    if (paramName.equals("guidance")) return w.asDouble();
                    if (paramName.contains("cfg") && !w.isIntegralNumber()) return w.asDouble();
                }
            }
        }
        return -1;
    }

    private String resolveStringParam(JsonNode node, String paramName, Map<Integer, JsonNode> nodeMap, Map<Integer, Integer> linkMap) {
        if (!node.has("inputs")) return null;
        for (JsonNode input : node.get("inputs")) {
            String name = input.get("name").asText().toLowerCase();
            if (name.contains(paramName)) {
                if (input.has("widget") && input.get("widget").has("value")) return input.get("widget").get("value").asText();
                if (input.has("link") && input.get("link").isInt()) {
                    int linkId = input.get("link").asInt();
                    if (linkMap.containsKey(linkId)) {
                        int sourceId = linkMap.get(linkId);
                        JsonNode source = nodeMap.get(sourceId);
                        if (source != null) {
                            if (source.has("widgets_values")) {
                                JsonNode w = source.get("widgets_values").get(0);
                                if (w != null && w.isTextual()) return w.asText();
                            }
                            if (source.has("inputs")) {
                                if (source.get("inputs").has("sampler_name")) return source.get("inputs").get("sampler_name").asText();
                                if (source.get("inputs").has("scheduler")) return source.get("inputs").get("scheduler").asText();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void extractModelFromList(JsonNode widgets, Map<String, String> results) {
        if (widgets == null) return;
        for (JsonNode w : widgets) {
            if (w.isTextual()) {
                String txt = w.asText();
                if (isValidModelFile(txt)) results.put("Model", cleanFilename(txt));
            }
        }
    }

    private long extractStepsFromWidgets(JsonNode widgets) {
        if (widgets == null) return -1;
        for (JsonNode w : widgets) {
            if (w.isBoolean()) continue;
            if (isInteger(w)) {
                long val = w.asLong();
                if (val > 1 && val <= 1000) return val;
            }
        }
        return -1;
    }

    private long extractSeedFromWidgets(JsonNode widgets) {
        if (widgets == null) return -1;
        for (JsonNode w : widgets) if (isNumeric(w)) { long val = w.asLong(); if (val > 1000000) return val; }
        return -1;
    }

    private double extractCfgFromWidgets(JsonNode widgets) {
        if (widgets == null) return -1;
        for (JsonNode w : widgets) if (isNumeric(w)) { double val = w.asDouble(); if (val > 0 && val <= 50.0) return val; }
        return -1;
    }

    private String extractKeyword(JsonNode widgets, Set<String> keywords) {
        if (widgets == null) return null;
        for (JsonNode w : widgets) if (w.isTextual() && keywords.stream().anyMatch(w.asText().toLowerCase()::contains)) return w.asText();
        return null;
    }

    private void extractPromptText(JsonNode node, JsonNode widgets, Map<String, String> results, String targetKey) {
        if (widgets == null) return;

        if (node.has("inputs")) {
            for (JsonNode input : node.get("inputs")) {
                String name = input.get("name").asText().toLowerCase();
                if ((name.equals("text") || name.equals("string") || name.equals("prompt")) &&
                        input.has("link") && !input.get("link").isNull()) {
                    return;
                }
            }
        }

        for (JsonNode w : widgets) {
            if (w.isTextual()) {
                String txt = w.asText().trim();
                if (isValidPrompt(txt)) {
                    appendResult(results, targetKey, txt);
                    extractLorasFromPrompt(txt, results);
                }
            }
        }
    }
}