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

    @Query("SELECT * FROM quizzes ORDER BY id DESC")
    List<Quiz> getAllQuizzes();

    @Query("SELECT * FROM quizzes ORDER BY id DESC LIMIT :limit")
    List<Quiz> getRecentQuizzes(int limit);

    @Delete
    void delete(Quiz quiz);

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    void deleteById(int quizId);

    // Quiz Result operations
    @Insert
    void insertResult(QuizResult result);

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    List<QuizResult> getAllResults();

    @Query("SELECT COUNT(*) FROM quiz_results")
    int getTotalQuizzesTaken();

    @Query("SELECT AVG(correctAnswers * 100.0 / totalQuestions) FROM quiz_results")
    double getAverageAccuracy();

    @Query("SELECT AVG(timeSpentMillis) FROM quiz_results")
    long getAverageTimeSpent();
}
