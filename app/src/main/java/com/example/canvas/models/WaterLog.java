package com.example.canvas.models; // Replace with your actual package

import java.util.Date;
import java.util.List;
import com.google.firebase.firestore.PropertyName;

public class WaterLog {

    private int totalIntake;
    private List<WaterEntry> entries;
    Date date;

    double amountLiters;
    public WaterLog() {
        // Required empty constructor for Firebase deserialization.
    }

    public WaterLog(int totalIntake, List<WaterEntry> entries) {
        this.totalIntake = totalIntake;
        this.entries = entries;
    }

    public int getTotalIntake() {
        return totalIntake;
    }

    public void setTotalIntake(int totalIntake) {
        this.totalIntake = totalIntake;
    }

    public List<WaterEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<WaterEntry> entries) {
        this.entries = entries;
    }

    public Date getDate() {
        return date; // Trả về giá trị của trường 'date'
    }

    public double getAmountLiters() {

        return amountLiters; // Trả về giá trị của trường 'amountLiters'
    }
}