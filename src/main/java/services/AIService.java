package services;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class AIService {

    // ⚠️ Replace this with your actual API Key
    private static final String API_KEY = "AIzaSyAiwccJYxcKa4L-VW1pM-WmQhQaDP-IUlw";

    // Initialize the Gemini Client
    private static final Client client = Client.builder()
            .apiKey(API_KEY)
            .build();

    public static String generateDescription(String title) {
        try {
            // 1. Prepare the optimized prompt
            String prompt = String.format(
                    "Write a catchy, enticing 2-sentence travel description for a trip titled '%s'. " +
                            "Make it sound exciting and professional. Keep it under 40 words. Do not use hashtags.",
                    title
            );

            // 2. Call Gemini 3 Flash (fastest model)
            // Note: We use "gemini-3-flash-preview" or the current stable version
            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    prompt,
                    null
            );

            // 3. Extract and return the text
            String result = response.text().trim();

            // Safety check: if AI returns empty
            return result.isEmpty() ? "Explore the beauty of " + title + " with us!" : result;

        } catch (Exception e) {
            System.err.println("AI Generation Error: " + e.getMessage());
            return "Discover the magic of " + title + "! A perfect blend of adventure and luxury awaits you.";
        }
    }
}