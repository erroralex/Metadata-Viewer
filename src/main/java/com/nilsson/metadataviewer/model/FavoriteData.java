package com.nilsson.metadataviewer.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FavoriteData {
    private String id;
    private String name;
    private String prompt;
    private String negative;
    private String model;
    private String software;
    private String sampler;
    private String steps;
    private String cfg;
    private String seed;
    private String size;
    private String loras;
    private String raw;
    private String thumbnailPath;
    private String originalPath;
    private String timestamp;

    public FavoriteData() {}

    public FavoriteData(String name, String prompt, String negative, String model, String software,
                        String sampler, String steps, String cfg, String seed, String size, String loras,
                        String raw, String originalPath) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.prompt = prompt;
        this.negative = negative;
        this.model = model;
        this.software = software;
        this.sampler = sampler;
        this.steps = steps;
        this.cfg = cfg;
        this.seed = seed;
        this.size = size;
        this.loras = loras;
        this.raw = raw;
        this.originalPath = originalPath;
        this.timestamp = LocalDateTime.now().toString();
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrompt() { return prompt; }
    public String getNegative() { return negative; }
    public String getModel() { return model; }
    public String getSoftware() { return software; }
    public String getSampler() { return sampler; }
    public String getSteps() { return steps; }
    public String getCfg() { return cfg; }
    public String getSeed() { return seed; }
    public String getSize() { return size; }
    public String getLoras() { return loras; }
    public String getRaw() { return raw; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getOriginalPath() { return originalPath; }
    public String getTimestamp() { return timestamp; }

    // --- Setters (Required for JSON Loading) ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setNegative(String negative) { this.negative = negative; }
    public void setModel(String model) { this.model = model; }
    public void setSoftware(String software) { this.software = software; }
    public void setSampler(String sampler) { this.sampler = sampler; }
    public void setSteps(String steps) { this.steps = steps; }
    public void setCfg(String cfg) { this.cfg = cfg; }
    public void setSeed(String seed) { this.seed = seed; }
    public void setSize(String size) { this.size = size; }
    public void setLoras(String loras) { this.loras = loras; }
    public void setRaw(String raw) { this.raw = raw; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}