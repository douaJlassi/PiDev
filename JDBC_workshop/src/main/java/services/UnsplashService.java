package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * UnsplashService — Fetches travel photos from the Unsplash API.
 *
 * SETUP (one-time):
 *   1. Go to unsplash.com/developers → "New Application"
 *   2. Copy your Access Key (starts with nothing special — just a long string)
 *   3. Add to src/main/resources/config.properties:
 *        unsplash.access.key=YOUR_ACCESS_KEY_HERE
 *      OR set environment variable:
 *        UNSPLASH_ACCESS_KEY=YOUR_ACCESS_KEY_HERE
 *
 * FREE TIER: 50 requests/hour — more than enough for a travel app.
 *
 * Usage:
 *   UnsplashService svc = new UnsplashService();
 *   svc.fetchPhotoUrl("Djerba Tunisia").thenAccept(url -> { ... });
 */
public class UnsplashService {

    private static final String API_BASE   = "https://api.unsplash.com";
    private static final int    TIMEOUT_MS = 10_000;

    // ── API key loaded once at class init ─────────────────────────────────────
    private static final String ACCESS_KEY;

    static {
        String key = null;

        // Priority 1: environment variable
        String env = System.getenv("UNSPLASH_ACCESS_KEY");
        if (env != null && !env.trim().isEmpty()) key = env.trim();

        // Priority 2: config.properties
        if (key == null) {
            try (InputStream in = UnsplashService.class
                    .getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    Properties props = new Properties();
                    props.load(in);
                    String fromFile = props.getProperty("unsplash.access.key", "").trim();
                    if (!fromFile.isEmpty() && !fromFile.equals("YOUR_ACCESS_KEY_HERE")) {
                        key = fromFile;
                    }
                }
            } catch (Exception ignored) { }
        }

        ACCESS_KEY = key;
        System.out.println("[UnsplashService] Key loaded: " + (ACCESS_KEY != null ? "YES" : "NO"));
    }

    /** True when a valid Unsplash access key is configured. */
    public static boolean isConfigured() {
        return ACCESS_KEY != null && !ACCESS_KEY.isEmpty();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches a high-quality travel photo URL for the given location query.
     *
     * Searches Unsplash with the query + "travel" appended for best results.
     * Returns the "regular" size URL (~1080px wide) — good quality without
     * being unnecessarily large.
     *
     * @param locationQuery e.g. "Djerba Tunisia", "Sidi Bou Said", "Tunis"
     * @return CompletableFuture with the photo URL string, or null if not found/error
     */
    public CompletableFuture<PhotoResult> fetchPhoto(String locationQuery) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConfigured()) {
                System.err.println("[UnsplashService] Not configured — add unsplash.access.key to config.properties");
                return null;
            }
            if (locationQuery == null || locationQuery.trim().isEmpty()) return null;
            try {
                return search(locationQuery.trim() + " travel");
            } catch (Exception e) {
                System.err.println("[UnsplashService] Error: " + e.getMessage());
                return null;
            }
        });
    }

    // ── Private implementation ────────────────────────────────────────────────

    private PhotoResult search(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        // per_page=5 so we can pick the first result with most relevant content
        String endpoint = API_BASE + "/search/photos?query=" + encoded
                + "&per_page=5&orientation=landscape&content_filter=high";

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Authorization", "Client-ID " + ACCESS_KEY);
        conn.setRequestProperty("Accept-Version", "v1");

        int code = conn.getResponseCode();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (code == 403) {
            System.err.println("[UnsplashService] 403 — invalid or rate-limited key");
            return null;
        }
        if (code == 401) {
            System.err.println("[UnsplashService] 401 — unauthorised, check your access key");
            return null;
        }
        if (code != 200) {
            System.err.println("[UnsplashService] HTTP " + code);
            return null;
        }

        JSONObject root    = new JSONObject(sb.toString());
        JSONArray  results = root.getJSONArray("results");
        if (results.isEmpty()) return null;

        JSONObject photo = results.getJSONObject(0);
        JSONObject urls  = photo.getJSONObject("urls");
        JSONObject user  = photo.getJSONObject("user");

        String regularUrl     = urls.getString("regular");  // ~1080px wide
        String photographerName = user.getString("name");
        String photographerLink = user.getJSONObject("links").getString("html");
        String altDescription   = photo.optString("alt_description", "Travel photo");

        return new PhotoResult(regularUrl, photographerName, photographerLink, altDescription);
    }

    // ── Result type ───────────────────────────────────────────────────────────

    /**
     * Unsplash photo result.
     * regularUrl  — direct image URL (~1080px wide, JPEG)
     * photographer — display name for attribution (required by Unsplash TOS)
     * photographerUrl — link to their Unsplash profile (required by TOS)
     * altDescription  — image description from Unsplash
     */
    public record PhotoResult(
            String regularUrl,
            String photographer,
            String photographerUrl,
            String altDescription
    ) { }
}