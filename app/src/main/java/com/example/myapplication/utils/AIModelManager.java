package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class AIModelManager {

    // Providers
    public static final String PROVIDER_GEMINI      = "Google Gemini";
    public static final String PROVIDER_GROQ        = "Groq";
    public static final String PROVIDER_OPENROUTER  = "OpenRouter";

    // Gemini model display names (hiển thị trên UI)
    public static final String[] MODELS_GEMINI_DISPLAY = {
        "Gemini-3.1-Flash-Lite-Preview",
        "Gemini-3-Flash-Preview",
        "Gemini-3.1-Pro-Preview",
        "Gemini-2.5-Pro",
        "Gemini-2.5-Flash",
        "Gemini-2.5-Flash-Lite",
        "Gemini-2.5-Computer-Use-Preview-10-2025",
        "Gemini-2.0-Flash",
        "Gemini-2.0-Flash-Lite",
        "Gemma-3-27b-It"
    };
    // Gemini model IDs thực tế gọi API (tương ứng theo index)
    public static final String[] MODELS_GEMINI_IDS = {
        "gemini-3.1-flash-lite-preview",
        "gemini-3-flash-preview",
        "gemini-3.1-pro-preview",
        "gemini-2.5-pro",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-computer-use-preview-10-2025",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite",
        "gemma-3-27b-it"
    };

    // Groq
    public static final String[] MODELS_GROQ = {
        "llama3-8b-8192",
        "llama-3.3-70b-versatile",
        "mixtral-8x7b-32768",
        "llama-3.1-8b-instant",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "meta-llama/llama-prompt-guard-2-22m",
        "meta-llama/llama-prompt-guard-2-86m",
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b",
        "openai/gpt-oss-safeguard-20b",
        "qwen/qwen3-32b",
        "groq/compound",
        "groq/compound-mini",
        "allam-2-7b",
        "canopylabs/orpheus-arabic-saudi",
        "canopylabs/orpheus-v1-english"
    };
    // OpenRouter
    public static final String[] MODELS_OPENROUTER = {
        "google/gemma-4-26b-a4b-it:free",
        "google/gemma-4-31b-it:free",
        "nvidia/nemotron-3-super-120b-a12b:free",
        "minimax/minimax-m2.5:free",
        "openrouter/auto:free",
        "arcee-ai/trinity-large-preview:free",
        "liquid/lfm-2.5-1.2b-thinking:free",
        "liquid/lfm-2.5-1.2b-instruct:free",
        "nvidia/nemotron-3-nano-30b-a3b:free",
        "nvidia/nemotron-nano-12b-v2-v1:free",
        "qwen/qwen3-next-80b-a3b-instruct:free",
        "nvidia/nemotron-nano-9b-v2:free",
        "openai/gpt-oss-120b:free",
        "openai/gpt-oss-20b:free",
        "z-ai/glm-4.5-air:free",
        "qwen/qwen3-coder:free",
        "cognitivecomputations/dolphin-mistral-24b-venice-edition:free",
        "google/gemma-3n-e2b-it:free",
        "google/gemma-3n-e4b-it:free",
        "google/gemma-3-4b-it:free",
        "google/gemma-3-12b-it:free",
        "google/gemma-3-27b-it:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "meta-llama/llama-3.2-3b-instruct:free",
        "nousresearch/hermes-3-llama-3.1-405b:free",
        "meta-llama/llama-3.1-8b-instruct:free",
        "mistralai/mistral-7b-instruct:free",
        "google/gemma-2-9b-it:free"
    };

    private static final String PREFS_NAME         = "ai_model_prefs";
    private static final String KEY_PROVIDER       = "selected_provider";
    private static final String KEY_MODEL          = "selected_model";
    private static final String KEY_GEMINI_KEYS    = "gemini_keys";
    private static final String KEY_GROQ_KEYS      = "groq_keys";
    private static final String KEY_OPENROUTER_KEYS= "openrouter_keys";
    private static final String KEY_GEMINI_IDX     = "gemini_key_idx";
    private static final String KEY_GROQ_IDX       = "groq_key_idx";
    private static final String KEY_OR_IDX         = "or_key_idx";

    private final SharedPreferences prefs;

    public AIModelManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String[] allProviders() {
        return new String[]{PROVIDER_GEMINI, PROVIDER_GROQ, PROVIDER_OPENROUTER};
    }

    /** Trả về tên hiển thị trên UI */
    public static String[] modelsForProvider(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:       return MODELS_GROQ;
            case PROVIDER_OPENROUTER: return MODELS_OPENROUTER;
            default:                  return MODELS_GEMINI_DISPLAY;
        }
    }

    /** Chuyển display name → API model ID thực tế */
    public static String toApiModelId(String provider, String displayName) {
        if (PROVIDER_GEMINI.equals(provider)) {
            // Thử match display name trước
            for (int i = 0; i < MODELS_GEMINI_DISPLAY.length; i++) {
                if (MODELS_GEMINI_DISPLAY[i].equals(displayName)) return MODELS_GEMINI_IDS[i];
            }
            // Thử match trực tiếp với ID (trường hợp đã lưu ID)
            for (String id : MODELS_GEMINI_IDS) {
                if (id.equals(displayName)) return displayName;
            }
            return MODELS_GEMINI_IDS[4]; // fallback: gemini-2.5-flash
        }
        return displayName;
    }

    // ── Provider / Model ────────────────────────────────────────────────────

    public String getSelectedProvider() {
        return prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI);
    }
    public void setSelectedProvider(String p) {
        prefs.edit().putString(KEY_PROVIDER, p).apply();
    }

    public String getSelectedModel() {
        String saved = prefs.getString(KEY_MODEL, MODELS_GEMINI_IDS[4]); // default: gemini-2.5-flash
        // Migration: nếu saved là display name cũ hoặc ID không hợp lệ → reset
        String provider = getSelectedProvider();
        if (PROVIDER_GEMINI.equals(provider)) {
            for (String id : MODELS_GEMINI_IDS) {
                if (id.equals(saved)) return saved;
            }
            for (String display : MODELS_GEMINI_DISPLAY) {
                if (display.equals(saved)) return saved;
            }
            // Không tìm thấy → reset về default
            String def = MODELS_GEMINI_DISPLAY[4]; // "Gemini-2.5-Flash"
            prefs.edit().putString(KEY_MODEL, def).apply();
            return def;
        }
        return saved;
    }
    public void setSelectedModel(String m) {
        prefs.edit().putString(KEY_MODEL, m).apply();
    }

    // ── Keys (round-robin) ──────────────────────────────────────────────────

    public List<String> getKeys(String provider) {
        String raw = prefs.getString(keysPrefKey(provider), "[]");
        List<String> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String k = arr.getString(i).trim();
                if (!k.isEmpty()) list.add(k);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void setKeys(String provider, List<String> keys) {
        try {
            JSONArray arr = new JSONArray();
            for (String k : keys) if (!k.trim().isEmpty()) arr.put(k.trim());
            prefs.edit()
                 .putString(keysPrefKey(provider), arr.toString())
                 .putInt(idxPrefKey(provider), 0)
                 .apply();
        } catch (Exception ignored) {}
    }

    /** Lấy key tiếp theo theo round-robin. Trả null nếu không có. */
    public String nextKey(String provider) {
        List<String> keys = getKeys(provider);
        if (keys.isEmpty()) return null;
        String idxKey = idxPrefKey(provider);
        int idx = prefs.getInt(idxKey, 0) % keys.size();
        prefs.edit().putInt(idxKey, (idx + 1) % keys.size()).apply();
        return keys.get(idx);
    }

    // ── Get key link per provider ────────────────────────────────────────────

    public static String getKeyUrl(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:       return "https://console.groq.com/keys";
            case PROVIDER_OPENROUTER: return "https://openrouter.ai/keys";
            default:                  return "https://aistudio.google.com/app/apikey";
        }
    }

    // ── API base URL ─────────────────────────────────────────────────────────

    public static String getBaseUrl(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:       return "https://api.groq.com/openai/v1/chat/completions";
            case PROVIDER_OPENROUTER: return "https://openrouter.ai/api/v1/chat/completions";
            default:                  return ""; // Gemini dùng URL riêng
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String keysPrefKey(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:       return KEY_GROQ_KEYS;
            case PROVIDER_OPENROUTER: return KEY_OPENROUTER_KEYS;
            default:                  return KEY_GEMINI_KEYS;
        }
    }

    private String idxPrefKey(String provider) {
        switch (provider) {
            case PROVIDER_GROQ:       return KEY_GROQ_IDX;
            case PROVIDER_OPENROUTER: return KEY_OR_IDX;
            default:                  return KEY_GEMINI_IDX;
        }
    }
}
