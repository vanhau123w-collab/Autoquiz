package com.example.myapplication.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.myapplication.data.Quiz;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class QuizExportManager {
    private static final String TAG = "QuizExportManager";

    public static String exportToJSON(Quiz quiz) {
        try {
            JSONObject json = new JSONObject();
            json.put("title", quiz.getTitle());
            json.put("count", quiz.getCount());
            json.put("jsonData", quiz.getJsonData());
            // We don't export userEmail or id to make it shareable
            return json.toString(4); // Indent for readability
        } catch (Exception e) {
            Log.e(TAG, "Error exporting quiz", e);
            return null;
        }
    }

    public static Quiz importFromJSON(String jsonString, String userEmail) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String title = json.getString("title");
            String count = json.getString("count");
            String jsonData = json.getString("jsonData");
            String date = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
            
            return new Quiz(title, date, count, jsonData, userEmail);
        } catch (Exception e) {
            Log.e(TAG, "Error importing quiz", e);
            return null;
        }
    }

    public static boolean writeToFile(Context context, Uri uri, String content) {
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing to file", e);
        }
        return false;
    }

    public static String readFromFile(Context context, Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading from file", e);
        }
        return null;
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        if (result != null && result.toLowerCase().endsWith(".json")) {
            result = result.substring(0, result.length() - 5);
        }
        return result;
    }
}
