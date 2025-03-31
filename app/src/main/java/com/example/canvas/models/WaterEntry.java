package com.example.canvas.models; // Replace with your actual package

import java.util.Date;

public class WaterEntry {

    private Date timestamp;
    private int amount;
    private String drinkType;

    public WaterEntry() {
        // Required empty constructor for Firebase deserialization.
    }

    public WaterEntry(Date timestamp, int amount, String drinkType) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.drinkType = drinkType;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDrinkType() {
        return drinkType;
    }

    public void setDrinkType(String drinkType) {
        this.drinkType = drinkType;
    }
}