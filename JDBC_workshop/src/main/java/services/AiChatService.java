package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * AiChatService — Groq Chat Completions client for the Rehletna AI Travel Assistant.
 *
 * SETUP (one-time):
 *   Add to src/main/resources/config.properties:
 *     groq.api.key=gsk_xxxxxxxxxxxxxxxxxxxxxxxx
 *   OR set environment variable:
 *     GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxxxxxx
 */
public class AiChatService {

    private static final String API_URL    = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL      = "llama-3.3-70b-versatile";
    private static final int    MAX_TOKENS = 600;
    private static final int    TIMEOUT_MS = 20_000;

    private static final String SYSTEM_PROMPT =
            "You are Rihla, a friendly and knowledgeable AI travel assistant for Rehletna — " +
                    "a Tunisian travel community platform. Your tone is warm, enthusiastic, and concise. " +
                    "You specialise in Tunisia (Tunis, Djerba, Sidi Bou Said, Sahara, Sousse, Carthage, " +
                    "Kairouan, Matmata, Tozeur, Tabarka, Hammamet) but handle all global travel questions confidently. " +
                    "Rules: " +
                    "(1) Keep replies concise: 3 to 6 sentences or a short bullet list. " +
                    "(2) Be specific: name real places, real dishes, practical tips. " +
                    "(3) For Tunisia questions, mention the best season to visit and one hidden gem. " +
                    "(4) For itinerary requests, structure clearly by day with suggested times. " +
                    "(5) End every reply with one short friendly follow-up question. " +
                    "(6) Never refuse a travel question. If asked something off-topic, answer briefly then steer back to travel.";

    // API key loaded once at class initialisation
    private static final String API_KEY;

    static {
        String key = null;

        // Priority 1: environment variable
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            key = envKey.trim();
        }

        // Priority 2: config.properties on classpath
        if (key == null) {
            try (InputStream in = AiChatService.class
                    .getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    String fileKey = props.getProperty("groq.api.key", "").trim();
                    if (fileKey.startsWith("gsk_")) key = fileKey;
                }
            } catch (Exception ignored) { }
        }

        API_KEY = key;

        // Debug log — remove once confirmed working
        System.out.println("[AiChatService] Groq key loaded: " + (API_KEY != null ? "YES" : "NO"));
    }

    /** True when a valid Groq key is available. */
    public static boolean isConfigured() {
        return API_KEY != null && API_KEY.startsWith("gsk_");
    }

    /**
     * Sends the full conversation history and returns the assistant reply asynchronously.
     * The caller owns the history list and must append both the user message before
     * calling and the returned assistant reply after it resolves.
     */
    public CompletableFuture<String> chat(List<Message> history) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConfigured()) {
                return "⚠ AI assistant not configured yet.\n\n" +
                        "Add your Groq key to config.properties:\n" +
                        "  groq.api.key=gsk_...\n\n" +
                        "Get a free key at console.groq.com";
            }
            try {
                return callApi(history);
            } catch (Exception e) {
                System.err.println("[AiChatService] " + e.getMessage());
                return "Sorry, I couldn't reach the AI service right now. " +
                        "Please check your connection and try again.";
            }
        });
    }

    private String callApi(List<Message> history) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
        for (Message m : history) {
            messages.put(new JSONObject().put("role", m.role()).put("content", m.content()));
        }

        String body = new JSONObject()
                .put("model", MODEL)
                .put("messages", messages)
                .put("max_tokens", MAX_TOKENS)
                .put("temperature", 0.8)
                .toString();

        HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream stream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (code == 200) {
            return new JSONObject(sb.toString())
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        }
        if (code == 401) return "⚠ Invalid Groq API key. Please update groq.api.key in config.properties.";
        if (code == 429) return "⚠ Rate limit reached. Please wait a moment and try again.";
        if (code == 503) return "⚠ Groq is temporarily unavailable. Please try again shortly.";
        try {
            return "⚠ " + new JSONObject(sb.toString()).getJSONObject("error").getString("message");
        } catch (Exception ignored) {
            return "⚠ Unexpected error (HTTP " + code + "). Please try again.";
        }
    }

    /** Immutable chat turn. role is "user" or "assistant". */
    public record Message(String role, String content) { }
}