package com.example.myapplication.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface QuizDao {
    @Insert
    void insert(Quiz quiz);

    @Query("SELECT * FROM quizzes WHERE userEmail = :email ORDER BY id DESC")
    List<Quiz> getAllQuizzes(String email);

    @Query("SELECT * FROM quizzes WHERE userEmail = :email ORDER BY id DESC LIMIT :limit")
    List<Quiz> getRecentQuizzes(String email, int limit);

    @Delete
    void delete(Quiz quiz);

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    void deleteById(int quizId);

    // Quiz Result operations
    @Insert
    void insertResult(QuizResult result);

    @Query("SELECT * FROM quiz_results WHERE userEmail = :email ORDER BY timestamp DESC")
    List<QuizResult> getAllResults(String email);

    @Query("SELECT COUNT(*) FROM quiz_results WHERE userEmail = :email")
    int getTotalQuizzesTaken(String email);

    @Query("SELECT AVG(correctAnswers * 100.0 / totalQuestions) FROM quiz_results WHERE userEmail = :email")
    double getAverageAccuracy(String email);

    @Query("SELECT AVG(timeSpentMillis) FROM quiz_results WHERE userEmail = :email")
    long getAverageTimeSpent(String email);
}
