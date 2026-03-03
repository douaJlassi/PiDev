package services;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

public class OffreAIService {


    private static final String API_KEY = "";

    // Initialize the Gemini Client
    private static final Client client = Client.builder()
            .apiKey(API_KEY)
            .build();

    public static String generateDescription(String title) {
        try {
            String prompt = String.format(
                    "Write a catchy, enticing 2-sentence travel description for a trip titled '%s'. " +
                            "Make it sound exciting and professional. Keep it under 40 words. Do not use hashtags.",
                    title
            );

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-3-flash-preview",
                    prompt,
                    null
            );

            String result = response.text().trim();

            return result.isEmpty() ? "Explore the beauty of " + title + " with us!" : result;

        } catch (Exception e) {
            System.err.println("AI Generation Error: " + e.getMessage());
            return "Discover the magic of " + title + "! A perfect blend of adventure and luxury awaits you.";
        }
    }
}