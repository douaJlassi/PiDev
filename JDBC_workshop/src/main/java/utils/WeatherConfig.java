package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * WeatherConfig - Centralized configuration for OpenWeatherMap API
 *
 * FREE ALTERNATIVE: Uses OpenWeatherMap free tier (1,000 calls/day)
 * - No credit card required
 * - Just email signup
 * - API key free forever
 *
 * SECURITY:
 * - API key stored in external config.properties file (NOT in source code)
 * - Supports environment variable override for production
 * - Falls back to demo key for quick testing (limited functionality)
 *
 * SETUP:
 * 1. Sign up at https://openweathermap.org/appid
 * 2. Verify email and get API key
 * 3. Add to config.properties: openweather.api.key=YOUR_KEY_HERE
 * 4. Wait 2 hours for key activation
 *
 * USAGE:
 * String apiKey = WeatherConfig.getApiKey();
 *
 * @see <a href="https://openweathermap.org/api">OpenWeatherMap API Docs</a>
 */
public class WeatherConfig {

    private static final String CONFIG_FILE = "config.properties";
    private static final String API_KEY_PROPERTY = "openweather.api.key";
    private static final String ENV_VAR_NAME = "OPENWEATHER_API_KEY";

    // Demo key for testing (limited calls, may expire)
    // Replace with your own key from https://openweathermap.org/appid
    private static final String DEMO_KEY = "demo_key_replace_with_yours";

    private static String cachedApiKey = null;
    private static boolean initialized = false;

    /**
     * Get OpenWeatherMap API key from (in priority order):
     * 1. Environment variable (production)
     * 2. config.properties file (development)
     * 3. Demo key (fallback for testing)
     */
    public static String getApiKey() {
        if (!initialized) {
            loadApiKey();
            initialized = true;
        }
        return cachedApiKey;
    }

    /**
     * Check if API is properly configured (not using demo key)
     */
    public static boolean isConfigured() {
        String key = getApiKey();
        return key != null && !key.equals(DEMO_KEY);
    }

    /**
     * Validate API key format (basic check)
     * OpenWeatherMap keys are 32 characters, alphanumeric
     */
    public static boolean isValidKeyFormat(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        // OpenWeatherMap keys are typically 32 chars, alphanumeric
        return key.length() == 32 && key.matches("[a-zA-Z0-9]+");
    }

    // ── Private helper methods ────────────────────────────────────────────────

    private static void loadApiKey() {
        // Priority 1: Environment variable (for production deployment)
        String envKey = System.getenv(ENV_VAR_NAME);
        if (envKey != null && !envKey.trim().isEmpty()) {
            cachedApiKey = envKey.trim();
            System.out.println("✓ OpenWeatherMap API key loaded from environment variable");
            return;
        }

        // Priority 2: config.properties file (for development)
        try (InputStream input = WeatherConfig.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String fileKey = prop.getProperty(API_KEY_PROPERTY);

                if (fileKey != null && !fileKey.trim().isEmpty()) {
                    cachedApiKey = fileKey.trim();
                    System.out.println("✓ OpenWeatherMap API key loaded from config.properties");
                    return;
                }
            }
        } catch (IOException e) {
            System.err.println("⚠ Could not load config.properties: " + e.getMessage());
        }

        // Priority 3: Demo key (fallback for quick testing)
        cachedApiKey = DEMO_KEY;
        System.err.println("⚠ Using demo weather API key - features will be limited!");
        System.err.println("  Get a free key at: https://openweathermap.org/appid");
        System.err.println("  Option 1: Set environment variable: " + ENV_VAR_NAME);
        System.err.println("  Option 2: Add to config.properties: " + API_KEY_PROPERTY);
    }

    /**
     * Reload API key (useful for testing or config changes)
     */
    public static void reload() {
        initialized = false;
        cachedApiKey = null;
        loadApiKey();
    }

    /**
     * Get base URL for OpenWeatherMap Current Weather API
     */
    public static String getBaseUrl() {
        return "https://api.openweathermap.org/data/2.5/weather";
    }

    /**
     * Get base URL for weather icons
     */
    public static String getIconUrl(String iconCode) {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }
}