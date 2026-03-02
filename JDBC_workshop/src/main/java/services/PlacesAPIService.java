package services;

import entities.PlaceAutocomplete;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PlacesAPIService - HTTP client for Nominatim (OpenStreetMap) Location Search
 *
 * FEATURES:
 * - Location autocomplete with debouncing support
 * - Asynchronous API calls (non-blocking UI)
 * - Error handling and fallbacks
 * - Response parsing from JSON
 * - 100% FREE - No API key required!
 *
 * API: Nominatim (OpenStreetMap)
 * - Endpoint: https://nominatim.openstreetmap.org
 * - Documentation: https://nominatim.org/release-docs/latest/api/Search/
 * - Usage Policy: Max 1 request/second, must include User-Agent
 * - Coverage: Excellent worldwide data, especially for Tunisia
 *
 * DEPENDENCIES:
 * - org.json library for JSON parsing (add to pom.xml/build.gradle)
 *
 * USAGE:
 * PlacesAPIService service = new PlacesAPIService();
 * service.getAutocompletePredictions("Tunis")
 *        .thenAccept(predictions -> {
 *            // Update UI with predictions
 *        });
 */
public class PlacesAPIService {

    private static final String NOMINATIM_SEARCH_URL =
            "https://nominatim.openstreetmap.org/search";

    private static final int TIMEOUT_MS = 5000; // 5 seconds (Nominatim can be slower than Google)
    private static final String USER_AGENT = "Rehletna.tn/1.0 (Travel Community Platform)";

    // Rate limiting: Nominatim requests max 1/second
    private static long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 1000;

    /**
     * Get autocomplete predictions for a search query (async)
     *
     * @param input User's search text (e.g., "Tun")
     * @return CompletableFuture with list of predictions
     */
    public CompletableFuture<List<PlaceAutocomplete>> getAutocompletePredictions(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate input
                if (input == null || input.trim().length() < 2) {
                    return new ArrayList<>();
                }

                // Rate limiting: ensure at least 1 second between requests
                enforceRateLimit();

                // Build URL with parameters
                String encodedInput = URLEncoder.encode(input.trim(), StandardCharsets.UTF_8);

                // Nominatim search parameters:
                // - q: search query
                // - format=json: return JSON
                // - addressdetails=1: include structured address (city, country, etc.)
                // - limit=5: max 5 results
                // - featuretype=city: prefer city-level results
                String urlString = String.format(
                        "%s?q=%s&format=json&addressdetails=1&limit=5",
                        NOMINATIM_SEARCH_URL, encodedInput
                );

                // Make HTTP request
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);

                // REQUIRED: User-Agent header (Nominatim usage policy)
                conn.setRequestProperty("User-Agent", USER_AGENT);

                // Read response
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    return parseNominatimResponse(response.toString());

                } else if (responseCode == 429) {
                    System.err.println("Nominatim rate limit exceeded. Please wait.");
                    return new ArrayList<>();

                } else {
                    System.err.println("Nominatim API error: HTTP " + responseCode);
                    return new ArrayList<>();
                }

            } catch (Exception e) {
                System.err.println("Failed to fetch autocomplete predictions: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Get autocomplete predictions (synchronous - for testing)
     */
    public List<PlaceAutocomplete> getAutocompletePredictionsSync(String input) {
        try {
            return getAutocompletePredictions(input).get();
        } catch (Exception e) {
            System.err.println("Sync autocomplete failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Private helper methods ────────────────────────────────────────────────

    /**
     * Enforce Nominatim rate limit (max 1 request/second)
     */
    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long timeSinceLastRequest = now - lastRequestTime;

        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            try {
                long sleepTime = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * Parse Nominatim JSON response
     *
     * Example response:
     * [
     *   {
     *     "place_id": 282402156,
     *     "osm_type": "relation",
     *     "osm_id": 192740,
     *     "lat": "36.8064948",
     *     "lon": "10.1815316",
     *     "display_name": "Tunis, Tunisia",
     *     "address": {
     *       "city": "Tunis",
     *       "country": "Tunisia",
     *       "country_code": "tn"
     *     },
     *     "type": "administrative"
     *   }
     * ]
     */
    private List<PlaceAutocomplete> parseNominatimResponse(String jsonResponse) {
        List<PlaceAutocomplete> results = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject location = jsonArray.getJSONObject(i);

                // Extract place_id (convert to string as Nominatim returns int)
                String placeId = String.valueOf(location.optLong("place_id", 0));

                // Extract display name
                String displayName = location.optString("display_name", "");

                // Extract structured address for better formatting
                String mainText = null;
                String secondaryText = null;

                JSONObject address = location.optJSONObject("address");
                if (address != null) {
                    // Try to get city/town/village name
                    mainText = address.optString("city");
                    if (mainText == null || mainText.isEmpty()) {
                        mainText = address.optString("town");
                    }
                    if (mainText == null || mainText.isEmpty()) {
                        mainText = address.optString("village");
                    }
                    if (mainText == null || mainText.isEmpty()) {
                        mainText = address.optString("state");
                    }

                    // Get country as secondary text
                    secondaryText = address.optString("country", "");
                }

                // Fallback: if no structured address, parse display_name
                if (mainText == null || mainText.isEmpty()) {
                    String[] parts = displayName.split(",");
                    if (parts.length >= 1) {
                        mainText = parts[0].trim();
                    }
                    if (parts.length >= 2) {
                        secondaryText = parts[parts.length - 1].trim(); // Last part is usually country
                    }
                }

                // Create PlaceAutocomplete object
                PlaceAutocomplete place = new PlaceAutocomplete(
                        placeId, displayName, mainText, secondaryText
                );

                if (place.isValid()) {
                    results.add(place);
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse Nominatim response: " + e.getMessage());
        }

        return results;
    }

    /**
     * Test connectivity to Nominatim API
     * @return true if API is reachable
     */
    public boolean testConnection() {
        try {
            List<PlaceAutocomplete> results = getAutocompletePredictionsSync("Paris");
            boolean success = !results.isEmpty();

            if (success) {
                System.out.println("✓ Nominatim API connection successful");
                System.out.println("  Found " + results.size() + " results for 'Paris'");
            }

            return success;
        } catch (Exception e) {
            System.err.println("✗ Nominatim connection test failed: " + e.getMessage());
            return false;
        }
    }
}