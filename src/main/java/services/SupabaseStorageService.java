package services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.Properties;

public class SupabaseStorageService {

    private String supabaseUrl;
    private String supabaseKey;
    private static final String BUCKET_NAME = "java_images";

    public SupabaseStorageService() {
        loadConfig();
    }

    private void loadConfig() {
        try (InputStream input = getClass().getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            supabaseUrl = prop.getProperty("supabase.url");       // e.g. https://xxxx.supabase.co
            supabaseKey = prop.getProperty("supabase.anon.key"); // your anon/service key
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String uploadImage(Path filePath, String fileName) throws IOException, InterruptedException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        String mimeType = Files.probeContentType(filePath); // e.g. "image/jpeg"
        if (mimeType == null) mimeType = "application/octet-stream";

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + fileName;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {

            return  fileName;
        } else {
            System.err.println("Upload failed: " + response.body());
            return null;
        }
    }

    public String getSignedUrl(String fileName, int expiresInSeconds) throws IOException, InterruptedException {
        String endpoint = supabaseUrl + "/storage/v1/object/sign/" + BUCKET_NAME + "/" + fileName;

        String body = "{\"expiresIn\": " + expiresInSeconds + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            org.json.JSONObject json = new org.json.JSONObject(response.body());
            String signedPath = json.getString("signedURL");
            String token = signedPath.substring(signedPath.indexOf("token=") + 6);
            return supabaseUrl + "/storage/v1/object/sign/" + BUCKET_NAME + "/" + fileName + "?token=" + token;

        } else {
            System.err.println("Failed to get signed URL: " + response.body());
            return null;
        }
    }





}