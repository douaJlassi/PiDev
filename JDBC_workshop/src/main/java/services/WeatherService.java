package services;

import entities.WeatherData;
import utils.WeatherCache;
import utils.WeatherConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WeatherService - HTTP client for OpenWeatherMap Current Weather API
 *
 * FEATURES:
 * - Fetch current weather by location name
 * - Asynchronous API calls (non-blocking UI)
 * - Automatic caching (30-minute TTL)
 * - Error handling and fallbacks
 * - Rate limit awareness
 *
 * API: OpenWeatherMap Current Weather
 * - Endpoint: https://api.openweathermap.org/data/2.5/weather
 * - Documentation: https://openweathermap.org/current
 * - Free tier: 1,000 calls/day
 * - Rate limit: 60 calls/minute
 *
 * USAGE:
 * WeatherService service = new WeatherService();
 * service.getWeatherByLocation("Tunis, Tunisia")
 *        .thenAccept(weather -> {
 *            if (weather != null) {
 *                // Display weather: ☀️ 28°C
 *            }
 *        });
 */
public class WeatherService {

    private final WeatherCache cache;
    private static final int TIMEOUT_MS = 5000; // 5 seconds

    public WeatherService() {
        this.cache = WeatherCache.getInstance();
    }

    /**
     * Get weather for a location (async, with caching)
     *
     * @param locationName Location string (e.g., "Tunis, Tunisia" or "Tunis,TN")
     * @return CompletableFuture with WeatherData or null if error
     */
    public CompletableFuture<WeatherData> getWeatherByLocation(String locationName) {
        // Check cache first
        WeatherData cached = cache.get(locationName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // Fetch from API
        return CompletableFuture.supplyAsync(() -> {
            try {
                WeatherData fresh = fetchWeatherSync(locationName);

                // Cache the result if valid
                if (fresh != null && fresh.isValid()) {
                    cache.put(locationName, fresh);
                }

                return fresh;

            } catch (Exception e) {
                System.err.println("Weather fetch error for '" + locationName + "': " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Get weather synchronously (blocking)
     * Mainly for testing or when async is not needed
     */
    public WeatherData getWeatherByLocationSync(String locationName) {
        try {
            return getWeatherByLocation(locationName).get();
        } catch (Exception e) {
            System.err.println("Sync weather fetch failed: " + e.getMessage());
            return null;
        }
    }

    // ── Private helper methods ────────────────────────────────────────────────

    private WeatherData fetchWeatherSync(String locationName) throws Exception {
        // Validate API key
        String apiKey = WeatherConfig.getApiKey();
        if (!WeatherConfig.isConfigured()) {
            System.err.println("⚠ Weather API key not configured - skipping weather fetch");
            return null;
        }

        // Build API URL
        String encodedLocation = URLEncoder.encode(locationName, StandardCharsets.UTF_8);
        String urlString = String.format(
                "%s?q=%s&appid=%s&units=metric&lang=en",
                WeatherConfig.getBaseUrl(),
                encodedLocation,
                apiKey
        );

        // Make HTTP request
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Rehletna.tn/1.0");

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
            return parseWeatherResponse(response.toString(), locationName);

        } else if (responseCode == 401) {
            System.err.println("⚠ Weather API key invalid or not activated yet (wait 2 hours after signup)");
            return null;

        } else if (responseCode == 404) {
            System.err.println("⚠ Location not found: " + locationName);
            return null;

        } else if (responseCode == 429) {
            System.err.println("⚠ Weather API rate limit exceeded");
            return null;

        } else {
            System.err.println("⚠ Weather API error: HTTP " + responseCode);
            return null;
        }
    }

    /**
     * Parse OpenWeatherMap JSON response
     *
     * Example response:
     * {
     *   "weather": [
     *     {
     *       "main": "Clear",
     *       "description": "clear sky",
     *       "icon": "01d"
     *     }
     *   ],
     *   "main": {
     *     "temp": 28.5,
     *     "feels_like": 29.2,
     *     "humidity": 45
     *   },
     *   "name": "Tunis"
     * }
     */
    private WeatherData parseWeatherResponse(String jsonResponse, String requestedLocation) {
        try {
            JSONObject json = new JSONObject(jsonResponse);

            // Extract weather info
            JSONArray weatherArray = json.optJSONArray("weather");
            if (weatherArray == null || weatherArray.length() == 0) {
                return null;
            }

            JSONObject weather = weatherArray.getJSONObject(0);
            String weatherMain = weather.optString("main", "Unknown");
            String weatherDescription = weather.optString("description", "");
            String iconCode = weather.optString("icon", "01d");

            // Extract temperature info
            JSONObject main = json.optJSONObject("main");
            if (main == null) {
                return null;
            }

            double temp = main.optDouble("temp", 0.0);
            double feelsLike = main.optDouble("feels_like", temp);
            int humidity = main.optInt("humidity", 0);

            // Extract location name (use API response name, fallback to requested)
            String locationName = json.optString("name", requestedLocation);

            // Build WeatherData object
            WeatherData data = new WeatherData(
                    locationName,
                    temp,
                    weatherMain,
                    weatherDescription,
                    iconCode
            );

            data.setFeelsLike(feelsLike);
            data.setHumidity(humidity);

            return data;

        } catch (Exception e) {
            System.err.println("Failed to parse weather response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Test connectivity to OpenWeatherMap API
     * @return true if API is reachable and key is valid
     */
    public boolean testConnection() {
        try {
            // Test with a well-known city
            WeatherData result = getWeatherByLocationSync("London,GB");
            boolean success = result != null && result.isValid();

            if (success) {
                System.out.println("✓ OpenWeatherMap API connection successful");
                System.out.println("  Weather in London: " + result.getFormattedTemperature() +
                        ", " + result.getWeatherMain());
            }

            return success;
        } catch (Exception e) {
            System.err.println("✗ Weather API connection test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get cache statistics
     */
    public WeatherCache.CacheStats getCacheStats() {
        return cache.getStats();
    }

    /**
     * Clear weather cache
     */
    public void clearCache() {
        cache.clear();
    }
}