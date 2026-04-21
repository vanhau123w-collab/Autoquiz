package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quizzes")
public class Quiz {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String date;
    private String count;
    private String jsonData;
    private String userEmail;

    public Quiz(String title, String date, String count, String jsonData, String userEmail) {
        this.title = title;
        this.date = date;
        this.count = count;
        this.jsonData = jsonData;
        this.userEmail = userEmail;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getCount() { return count; }
    public void setCount(String count) { this.count = count; }
    public String getJsonData() { return jsonData; }
    public void setJsonData(String jsonData) { this.jsonData = jsonData; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
