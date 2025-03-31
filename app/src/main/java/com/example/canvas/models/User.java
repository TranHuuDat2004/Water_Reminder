package com.example.canvas.models; // Replace with your actual package

import java.util.Date;

public class User {

    private String username;
    private String email;
    private String phoneNumber;
    private Date registrationDate;
    private Integer calories;  // Directly store individual values
    private Integer heart;
    private Integer sleep;
    private Integer walk;

    public User() {
        // Required empty constructor for Firebase deserialization.
    }

    public User(String username, String email, String phoneNumber, Date registrationDate, Integer calories, Integer heart, Integer sleep, Integer walk) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.registrationDate = registrationDate;
        this.calories = calories;
        this.heart = heart;
        this.sleep = sleep;
        this.walk = walk;
    }

    // Constructor for when not creating the registrationDate
    public User(String username, String email, String phoneNumber, Integer calories, Integer heart, Integer sleep, Integer walk) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.calories = calories;
        this.heart = heart;
        this.sleep = sleep;
        this.walk = walk;
    }


    // Constructor for when not creating the registrationDate
    public User(String username, String email, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Integer getHeart() {
        return heart;
    }

    public void setHeart(Integer heart) {
        this.heart = heart;
    }

    public Integer getSleep() {
        return sleep;
    }

    public void setSleep(Integer sleep) {
        this.sleep = sleep;
    }

    public Integer getWalk() {
        return walk;
    }

    public void setWalk(Integer walk) {
        this.walk = walk;
    }
}