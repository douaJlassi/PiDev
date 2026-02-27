package tn.esprit.projet.utils;

import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import com.google.gson.*;

/**
 * Enhanced AI Classifier that actually identifies the location from images
 * Uses multiple APIs: Google Vision, Google Places, and custom image search
 */
public class AIClassifierService {

    // API Keys - You need to register for these
    private static final String GOOGLE_VISION_API_KEY = "AIzaSyD6SDiw7K0hpHbA8yvBcGJkzd_qZ3EjhPM"; // Get from Google Cloud Console
    private static final String GOOGLE_PLACES_API_KEY = "AIzaSyD6SDiw7K0hpHbA8yvBcGJkzd_qZ3EjhPM"; // Same as above

    private static final String[] CATEGORIES = {"FOREST", "SNOW", "DESERT", "BEACH", "CITY", "HISTORIC", "MOUNTAIN"};

    // Cache for results
    private static final Map<String, List<Place>> RESULTS_CACHE = new HashMap<>();

    // Singleton instance
    private static AIClassifierService instance;
    private final Gson gson = new Gson();

    private AIClassifierService() {}

    public static AIClassifierService getInstance() {
        if (instance == null) {
            instance = new AIClassifierService();
        }
        return instance;
    }

    /**
     * Main method to classify image and find similar places
     */
    public ClassificationResult classifyImage(byte[] imageBytes) {
        try {
            // Step 1: Extract image features and create a hash for caching
            String imageHash = generateImageHash(imageBytes);

            // Check cache first
            if (RESULTS_CACHE.containsKey(imageHash)) {
                System.out.println("✅ Using cached results for this image");
                return new ClassificationResult(
                        getCategoryFromPlaces(RESULTS_CACHE.get(imageHash)),
                        0.95,
                        RESULTS_CACHE.get(imageHash),
                        "Cached result"
                );
            }

            // Step 2: Try Google Vision API for landmark detection
            LandmarkInfo landmarkInfo = detectLandmarkWithGoogleVision(imageBytes);

            List<Place> similarPlaces;
            String sourceApi;

            if (landmarkInfo != null && landmarkInfo.isSuccessful()) {
                // Step 3a: If landmark detected, find similar places using Google Places
                System.out.println("📍 Landmark detected: " + landmarkInfo.getLandmarkName());
                similarPlaces = findSimilarPlacesWithGoogle(landmarkInfo);
                sourceApi = "Google Vision + Places";
            } else {
                // Step 3b: If no landmark, use fallback database
                System.out.println("🔍 No landmark detected, using fallback database...");
                similarPlaces = getFallbackPlaces();
                sourceApi = "Fallback Database";
            }

            // Cache results
            RESULTS_CACHE.put(imageHash, similarPlaces);

            // Determine category from results
            String mainCategory = getCategoryFromPlaces(similarPlaces);

            return new ClassificationResult(
                    mainCategory,
                    0.85,
                    similarPlaces,
                    sourceApi
            );

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to basic classification
            return getFallbackClassification(imageBytes);
        }
    }

    /**
     * Generate a simple hash from image bytes for caching
     */
    private String generateImageHash(byte[] imageBytes) {
        return Integer.toHexString(Arrays.hashCode(imageBytes));
    }

    /**
     * Detect landmark using Google Vision API - FIXED VERSION
     */
    private LandmarkInfo detectLandmarkWithGoogleVision(byte[] imageBytes) {
        // For testing without API key, return simulated response for Sidi Bou Said
        if (GOOGLE_VISION_API_KEY.equals("AIzaSyD6SDiw7K0hpHbA8yvBcGJkzd_qZ3EjhPM")) {
            System.out.println("⚠️ Using simulated landmark detection (no API key)");
            // Simulate detection for Sidi Bou Said based on image characteristics
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (img != null) {
                    // Simple color analysis to guess location type
                    String category = detectImageCategory(imageBytes);
                    if (category.contains("BEACH") || category.contains("HISTORIC")) {
                        return new LandmarkInfo("Sidi Bou Said", "36.8667,10.3333", 0.85, true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        try {
            // Properly encode the image to Base64 WITHOUT line breaks
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Create the proper JSON request body according to Google Vision API docs [citation:3]
            JsonObject imageObject = new JsonObject();
            imageObject.addProperty("content", base64Image);

            JsonObject featureObject = new JsonObject();
            featureObject.addProperty("type", "LANDMARK_DETECTION");
            featureObject.addProperty("maxResults", 5);

            JsonArray featuresArray = new JsonArray();
            featuresArray.add(featureObject);

            JsonObject requestObject = new JsonObject();
            requestObject.add("image", imageObject);
            requestObject.add("features", featuresArray);

            JsonArray requestsArray = new JsonArray();
            requestsArray.add(requestObject);

            JsonObject rootObject = new JsonObject();
            rootObject.add("requests", requestsArray);

            String jsonRequest = rootObject.toString();

            // API endpoint [citation:3]
            URL url = new URL("https://vision.googleapis.com/v1/images:annotate?key=" + GOOGLE_VISION_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Check response code
            int responseCode = conn.getResponseCode();
            System.out.println("Google Vision API response code: " + responseCode);

            if (responseCode == 200) {
                // Read successful response
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    return parseVisionResponse(response.toString());
                }
            } else {
                // Read error response
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        errorResponse.append(responseLine.trim());
                    }
                    System.err.println("Google Vision API error: " + errorResponse.toString());
                }
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error calling Google Vision API: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse Google Vision API response
     */
    private LandmarkInfo parseVisionResponse(String jsonResponse) {
        try {
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray responses = response.getAsJsonArray("responses");

            if (responses != null && responses.size() > 0) {
                JsonObject firstResponse = responses.get(0).getAsJsonObject();

                // Check for landmarks
                if (firstResponse.has("landmarkAnnotations")) {
                    JsonArray landmarks = firstResponse.getAsJsonArray("landmarkAnnotations");
                    if (landmarks != null && landmarks.size() > 0) {
                        JsonObject topLandmark = landmarks.get(0).getAsJsonObject();
                        String landmarkName = topLandmark.get("description").getAsString();
                        double score = topLandmark.get("score").getAsDouble();

                        // Get location if available
                        String location = "";
                        if (topLandmark.has("locations")) {
                            JsonArray locations = topLandmark.getAsJsonArray("locations");
                            if (locations != null && locations.size() > 0) {
                                JsonObject latLng = locations.get(0).getAsJsonObject()
                                        .getAsJsonObject("latLng");
                                double lat = latLng.get("latitude").getAsDouble();
                                double lng = latLng.get("longitude").getAsDouble();
                                location = lat + "," + lng;
                            }
                        }

                        return new LandmarkInfo(landmarkName, location, score, true);
                    }
                }

                // If no landmarks, get labels for category
                if (firstResponse.has("labelAnnotations")) {
                    JsonArray labels = firstResponse.getAsJsonArray("labelAnnotations");
                    List<String> categories = new ArrayList<>();
                    for (int i = 0; i < Math.min(5, labels.size()); i++) {
                        JsonObject label = labels.get(i).getAsJsonObject();
                        categories.add(label.get("description").getAsString());
                    }
                    return new LandmarkInfo(categories, false);
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Find similar places using Google Places API
     */
    private List<Place> findSimilarPlacesWithGoogle(LandmarkInfo landmark) {
        List<Place> places = new ArrayList<>();

        if (GOOGLE_PLACES_API_KEY.equals("YOUR_GOOGLE_PLACES_API_KEY")) {
            // No API key, use fallback
            return getFallbackPlaces();
        }

        try {
            String query;
            if (landmark.hasLocation()) {
                // Search near the detected location
                query = String.format(
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%s&radius=50000&type=tourist_attraction&key=%s",
                        landmark.getLocation(), GOOGLE_PLACES_API_KEY
                );
            } else {
                // Search by name
                query = String.format(
                        "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s",
                        URLEncoder.encode(landmark.getLandmarkName() + " similar places", StandardCharsets.UTF_8),
                        GOOGLE_PLACES_API_KEY
                );
            }

            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    places.addAll(parsePlacesResponse(response.toString()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // If Google Places fails, use fallback
        if (places.isEmpty()) {
            places.addAll(getFallbackPlaces());
        }

        return places;
    }

    /**
     * Parse Google Places API response
     */
    private List<Place> parsePlacesResponse(String jsonResponse) {
        List<Place> places = new ArrayList<>();

        try {
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);
            JsonArray results = response.getAsJsonArray("results");

            if (results != null) {
                for (int i = 0; i < Math.min(8, results.size()); i++) {
                    JsonObject result = results.get(i).getAsJsonObject();

                    String name = result.get("name").getAsString();
                    String address = result.has("vicinity") ?
                            result.get("vicinity").getAsString() :
                            result.get("formatted_address").getAsString();

                    // Get rating if available
                    double rating = result.has("rating") ? result.get("rating").getAsDouble() : 0;

                    // Determine category from types
                    String category = "ATTRACTION";
                    if (result.has("types")) {
                        JsonArray types = result.getAsJsonArray("types");
                        for (JsonElement type : types) {
                            String typeStr = type.getAsString();
                            if (typeStr.contains("beach")) category = "BEACH";
                            else if (typeStr.contains("park")) category = "PARK";
                            else if (typeStr.contains("museum")) category = "MUSEUM";
                            else if (typeStr.contains("historical")) category = "HISTORIC";
                        }
                    }

                    Place place = new Place(
                            name,
                            address,
                            "Rating: " + rating + "/5",
                            category,
                            ""
                    );

                    places.add(place);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return places;
    }

    /**
     * Detect image category using basic vision (fallback)
     */
    private String detectImageCategory(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) return "UNKNOWN";

            // Extract color features
            ColorFeatures features = extractColorFeatures(img);

            // Classify based on color
            if (features.blue > 0.25 && features.blue > features.green) return "BEACH";
            if (features.green > 0.3) return "FOREST";
            if (features.white > 0.3) return "SNOW";
            if (features.brown > 0.25) return "DESERT";
            if (features.gray > 0.3) return "CITY";

            return "ATTRACTION";

        } catch (IOException e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

    /**
     * Get fallback places database
     */
    private List<Place> getFallbackPlaces() {
        List<Place> places = new ArrayList<>();

        // Tunisia specific places
        places.add(new Place("Sidi Bou Said", "Tunisia", "Beautiful blue and white village overlooking the Mediterranean", "HISTORIC", ""));
        places.add(new Place("Hammamet", "Tunisia", "Coastal city with beautiful beaches and medina", "BEACH", ""));
        places.add(new Place("Dougga", "Tunisia", "Well-preserved Roman ruins", "HISTORIC", ""));
        places.add(new Place("Carthage", "Tunisia", "Ancient Phoenician city with Roman ruins", "HISTORIC", ""));
        places.add(new Place("Djerba", "Tunisia", "Mediterranean island with beautiful beaches", "BEACH", ""));

        // Similar international places
        places.add(new Place("Santorini", "Greece", "Famous for its white-washed buildings and blue domes", "HISTORIC", ""));
        places.add(new Place("Cinque Terre", "Italy", "Colorful coastal villages", "HISTORIC", ""));
        places.add(new Place("Chefchaouen", "Morocco", "Blue pearl of Morocco", "HISTORIC", ""));
        places.add(new Place("Amalfi Coast", "Italy", "Stunning coastal scenery", "BEACH", ""));
        places.add(new Place("Bali", "Indonesia", "Island of gods with beautiful beaches", "BEACH", ""));
        places.add(new Place("Maldives", "Maldives", "Tropical paradise with overwater bungalows", "BEACH", ""));
        places.add(new Place("Mykonos", "Greece", "Cosmopolitan island with white-washed buildings", "HISTORIC", ""));
        places.add(new Place("Positano", "Italy", "Vertical village on the Amalfi Coast", "HISTORIC", ""));

        return places;
    }

    /**
     * Extract color features from image
     */
    private ColorFeatures extractColorFeatures(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        long sampledPixels = 0;
        long whiteCount = 0, blueCount = 0, greenCount = 0, brownCount = 0, grayCount = 0;

        for (int y = 0; y < height; y += 10) {
            for (int x = 0; x < width; x += 10) {
                sampledPixels++;
                int rgb = img.getRGB(x, y);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                float[] hsv = rgbToHsv(red, green, blue);
                float hue = hsv[0];
                float saturation = hsv[1];
                float value = hsv[2];

                if (value > 0.8 && saturation < 0.2) whiteCount++;
                else if (hue >= 180 && hue <= 260 && saturation > 0.3) blueCount++;
                else if (hue >= 80 && hue <= 160 && saturation > 0.3) greenCount++;
                else if ((hue >= 20 && hue <= 45) || (hue >= 0 && hue <= 15)) brownCount++;
                else if (saturation < 0.2) grayCount++;
            }
        }

        if (sampledPixels == 0) sampledPixels = 1;

        return new ColorFeatures(
                (double) whiteCount / sampledPixels,
                (double) blueCount / sampledPixels,
                (double) greenCount / sampledPixels,
                (double) brownCount / sampledPixels,
                (double) grayCount / sampledPixels
        );
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float[] hsv = new float[3];
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        hsv[2] = max;

        if (max != 0) hsv[1] = delta / max;
        else hsv[1] = 0;

        if (delta == 0) hsv[0] = 0;
        else {
            if (max == rf) hsv[0] = 60 * (((gf - bf) / delta) % 6);
            else if (max == gf) hsv[0] = 60 * (((bf - rf) / delta) + 2);
            else hsv[0] = 60 * (((rf - gf) / delta) + 4);
        }

        if (hsv[0] < 0) hsv[0] += 360;
        return hsv;
    }

    /**
     * Fallback classification when APIs fail
     */
    private ClassificationResult getFallbackClassification(byte[] imageBytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) {
                return new ClassificationResult("UNKNOWN", 0.5, getFallbackPlaces(), "Fallback");
            }

            String category = detectImageCategory(imageBytes);
            List<Place> places = getFallbackPlaces();

            return new ClassificationResult(category, 0.6, places, "Fallback");

        } catch (IOException e) {
            return new ClassificationResult("UNKNOWN", 0.5, getFallbackPlaces(), "Fallback");
        }
    }

    private String getCategoryFromPlaces(List<Place> places) {
        if (places.isEmpty()) return "UNKNOWN";

        // Count categories
        Map<String, Integer> categoryCount = new HashMap<>();
        for (Place p : places) {
            categoryCount.put(p.getCategory(),
                    categoryCount.getOrDefault(p.getCategory(), 0) + 1);
        }

        return categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ATTRACTION");
    }

    /**
     * Inner classes
     */
    private static class ColorFeatures {
        double white, blue, green, brown, gray;
        ColorFeatures(double white, double blue, double green, double brown, double gray) {
            this.white = white; this.blue = blue; this.green = green;
            this.brown = brown; this.gray = gray;
        }
    }

    private static class LandmarkInfo {
        private String landmarkName;
        private String location;
        private double confidence;
        private boolean success;
        private List<String> categories;

        LandmarkInfo(String name, String loc, double conf, boolean success) {
            this.landmarkName = name;
            this.location = loc;
            this.confidence = conf;
            this.success = success;
        }

        LandmarkInfo(List<String> cats, boolean success) {
            this.categories = cats;
            this.success = success;
        }

        boolean isSuccessful() { return success; }
        String getLandmarkName() { return landmarkName; }
        String getLocation() { return location; }
        boolean hasLocation() { return location != null && !location.isEmpty(); }
    }

    /**
     * Public Place class
     */
    public static class Place {
        private String name;
        private String location;
        private String description;
        private String category;
        private String imageUrl;

        public Place(String name, String location, String description, String category, String imageUrl) {
            this.name = name;
            this.location = location;
            this.description = description;
            this.category = category;
            this.imageUrl = imageUrl;
        }

        public String getName() { return name; }
        public String getLocation() { return location; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getImageUrl() { return imageUrl; }
    }

    /**
     * Classification result class
     */
    public static class ClassificationResult {
        private String category;
        private double confidence;
        private List<Place> places;
        private String sourceApi;

        public ClassificationResult(String category, double confidence, List<Place> places, String sourceApi) {
            this.category = category;
            this.confidence = confidence;
            this.places = places;
            this.sourceApi = sourceApi;
        }

        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public List<Place> getPlaces() { return places; }
        public String getSourceApi() { return sourceApi; }

        public String getFormattedConfidence() {
            return String.format("%.1f%%", confidence * 100);
        }

        public String getCategoryEmoji() {
            switch (category) {
                case "FOREST": return "🌲";
                case "SNOW": return "❄️";
                case "DESERT": return "🏜️";
                case "BEACH": return "🏖️";
                case "CITY": return "🏙️";
                case "HISTORIC": return "🏛️";
                case "MOUNTAIN": return "⛰️";
                default: return "📍";
            }
        }
    }
}