package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(tableName = "quiz_results",
        foreignKeys = @ForeignKey(entity = Quiz.class,
                parentColumns = "id",
                childColumns = "quizId",
                onDelete = ForeignKey.CASCADE))
public class QuizResult {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int quizId;
    private int correctAnswers;
    private int totalQuestions;
    private long timeSpentMillis;
    private String userEmail;
    private long timestamp;

    public QuizResult(int quizId, int correctAnswers, int totalQuestions, long timeSpentMillis, long timestamp, String userEmail) {
        this.quizId = quizId;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.timeSpentMillis = timeSpentMillis;
        this.timestamp = timestamp;
        this.userEmail = userEmail;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public long getTimeSpentMillis() { return timeSpentMillis; }
    public void setTimeSpentMillis(long timeSpentMillis) { this.timeSpentMillis = timeSpentMillis; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
}
