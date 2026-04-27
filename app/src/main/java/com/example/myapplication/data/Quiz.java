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
    private int lastPosition = 0; // Lưu câu đang làm dở
    private String lastProgress;  // Lưu JSON đáp án đã chọn
    private String completedResults; // Lưu JSON đáp án đã hoàn thành để xem lại sau
    private String aiEvaluation; // Lưu kết quả đánh giá của AI (chỉ 1 lần)

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

    public int getLastPosition() { return lastPosition; }
    public void setLastPosition(int lastPosition) { this.lastPosition = lastPosition; }
    public String getLastProgress() { return lastProgress; }
    public void setLastProgress(String lastProgress) { this.lastProgress = lastProgress; }
    public String getCompletedResults() { return completedResults; }
    public void setCompletedResults(String completedResults) { this.completedResults = completedResults; }
    public String getAiEvaluation() { return aiEvaluation; }
    public void setAiEvaluation(String aiEvaluation) { this.aiEvaluation = aiEvaluation; }
}
