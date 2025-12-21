package com.nilsson.metadataviewer.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class FavoriteData {
    private String id;
    private String name;
    private String prompt;
    private String negative;
    private String model;
    private String sampler;
    private String steps;
    private String cfg;
    private String seed;
    private String loras;
    private String raw;
    private String thumbnailPath;
    private String originalPath;
    private String timestamp;

    public FavoriteData() {}

    public FavoriteData(String name, String prompt, String negative, String model, String sampler,
                        String steps, String cfg, String seed, String loras, String raw,
                        String originalPath) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.prompt = prompt;
        this.negative = negative;
        this.model = model;
        this.sampler = sampler;
        this.steps = steps;
        this.cfg = cfg;
        this.seed = seed;
        this.loras = loras;
        this.raw = raw;
        this.originalPath = originalPath;
        this.timestamp = LocalDateTime.now().toString();
    }

    // Getters and Setters for all fields (id, name, prompt, negative, model, sampler, steps, cfg, seed, loras, raw, thumbnailPath, originalPath, timestamp)
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrompt() { return prompt; }
    public String getNegative() { return negative; }
    public String getModel() { return model; }
    public String getSampler() { return sampler; }
    public String getSteps() { return steps; }
    public String getCfg() { return cfg; }
    public String getSeed() { return seed; }
    public String getLoras() { return loras; }
    public String getRaw() { return raw; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String path) { this.thumbnailPath = path; }
    public String getOriginalPath() { return originalPath; }
    public String getTimestamp() { return timestamp; }
}