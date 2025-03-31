package com.example.canvas.models; // Replace with your actual package

import java.util.Date;

public class Achievement {

    private String name;
    private String description;
    private Date dateEarned;
    private String badgeImage;
    private boolean completed;
    private int progress;

    public Achievement() {
        // Required empty constructor for Firebase deserialization.
    }

    public Achievement(String name, String description, Date dateEarned, String badgeImage, boolean completed, int progress) {
        this.name = name;
        this.description = description;
        this.dateEarned = dateEarned;
        this.badgeImage = badgeImage;
        this.completed = completed;
        this.progress = progress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateEarned() {
        return dateEarned;
    }

    public void setDateEarned(Date dateEarned) {
        this.dateEarned = dateEarned;
    }

    public String getBadgeImage() {
        return badgeImage;
    }

    public void setBadgeImage(String badgeImage) {
        this.badgeImage = badgeImage;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}