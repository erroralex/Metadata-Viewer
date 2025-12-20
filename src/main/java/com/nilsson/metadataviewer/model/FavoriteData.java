package com.nilsson.metadataviewer.model;

import java.time.LocalDateTime;
import java.util.UUID;

// POJO for storing a favorited metadata prompt with image reference.
public class FavoriteData {
    private String id;
    private String name;
    private String prompt;
    private String model;
    private String steps;
    private String loras;
    private String imagePath; // New field for thumbnail support
    private String timestamp;

    public FavoriteData() {}

    public FavoriteData(String name, String prompt, String model, String steps, String loras, String imagePath) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.prompt = prompt;
        this.model = model;
        this.steps = steps;
        this.loras = loras;
        this.imagePath = imagePath;
        this.timestamp = LocalDateTime.now().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }
    public String getLoras() { return loras; }
    public void setLoras(String loras) { this.loras = loras; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getTimestamp() { return timestamp; }
}