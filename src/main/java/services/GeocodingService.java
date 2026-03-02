package services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GeocodingService — resolves a human-readable place name to geographic
 * coordinates (latitude / longitude) using the Nominatim API.
 *
 * ═══════════════════════════════════════════════════════════
 * WHY A SEPARATE SERVICE FROM PlacesAPIService?
 * ═══════════════════════════════════════════════════════════
 * PlacesAPIService is designed for incremental autocomplete — it returns
 * structured address objects (PlaceAutocomplete) for list display.
 * GeocodingService is purpose-built for coordinate resolution:
 *   - Returns a simple double[]{lat, lon} pair
 *   - Maintains an in-memory cache (so the same place doesn't hit
 *     the network twice in the same session)
 *   - Used exclusively by MapController to position map markers
 *
 * ═══════════════════════════════════════════════════════════
 * API USED: Nominatim (OpenStreetMap)
 * ═══════════════════════════════════════════════════════════
 * Endpoint: https://nominatim.openstreetmap.org/search
 * Cost:     100% FREE. No API key. No billing address.
 * Limit:    Max 1 request/second (enforced by enforceRateLimit()).
 * Policy:   Must send a descriptive User-Agent header.
 *
 * Example request:
 *   GET https://nominatim.openstreetmap.org/search
 *       ?q=Tunis&format=json&limit=1
 *
 * Example response:
 *   [{"lat":"36.8064948","lon":"10.1815316","display_name":"Tunis..."}]
 *
 * ═══════════════════════════════════════════════════════════
 * CACHING STRATEGY
 * ═══════════════════════════════════════════════════════════
 * Results are stored in a ConcurrentHashMap keyed by lower-cased place
 * name. Cache is session-scoped (lives as long as the JVM).
 * Max 500 entries — sufficient for any single user session.
 * No TTL needed: city coordinates don't change.
 *
 * Cache key: place name lowercased and trimmed
 * Cache value: double[] {latitude, longitude}
 *
 * ═══════════════════════════════════════════════════════════
 * USAGE
 * ═══════════════════════════════════════════════════════════
 * GeocodingService svc = new GeocodingService();
 *
 * // Async (preferred — keeps UI thread free)
 * svc.geocode("Tunis").thenAccept(coords -> {
 *     if (coords != null) {
 *         double lat = coords[0];
 *         double lon = coords[1];
 *     }
 * });
 *
 * // Sync (only use off the UI thread)
 * double[] coords = svc.geocodeSync("Sfax");
 */
public class GeocodingService {

    // ── Nominatim constants ───────────────────────────────────────────────────
    private static final String BASE_URL   = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "Rehletna.tn/1.0 (Travel Community Platform)";
    private static final int    TIMEOUT_MS = 6000;

    // ── Rate limiting ─────────────────────────────────────────────────────────
    // Nominatim requires at most 1 request per second.
    // We share the rate limiter across all instances via a static field.
    private static long         lastRequestTime = 0;
    private static final long   MIN_INTERVAL_MS = 1100; // slight buffer over 1000

    // ── Session cache ─────────────────────────────────────────────────────────
    // Shared across all instances (static). Thread-safe map.
    private static final ConcurrentHashMap<String, double[]> cache =
            new ConcurrentHashMap<>(64);
    private static final int MAX_CACHE_SIZE = 500;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolve a place name to {latitude, longitude} asynchronously.
     *
     * @param placeName  Human-readable place name, e.g. "Sousse, Tunisia"
     * @return CompletableFuture resolving to double[]{lat, lon},
     *         or null if geocoding fails (unknown place, network error, etc.)
     */
    public CompletableFuture<double[]> geocode(String placeName) {
        return CompletableFuture.supplyAsync(() -> geocodeSync(placeName));
    }

    /**
     * Synchronous version — call only from a background thread.
     * Returns null on failure so callers can skip the pin gracefully.
     */
    public double[] geocodeSync(String placeName) {
        if (placeName == null || placeName.trim().isEmpty()) return null;

        String key = placeName.trim().toLowerCase();

        // 1. Cache hit — skip network
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // 2. Network fetch
        try {
            enforceRateLimit();

            String encoded = URLEncoder.encode(placeName.trim(), StandardCharsets.UTF_8);
            String urlStr  = BASE_URL + "?q=" + encoded + "&format=json&limit=1";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);

            int status = conn.getResponseCode();

            if (status == 200) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                double[] coords = parseCoords(sb.toString());

                // 3. Store in cache (if still under limit)
                if (coords != null && cache.size() < MAX_CACHE_SIZE) {
                    cache.put(key, coords);
                }

                return coords;

            } else if (status == 429) {
                System.err.println("[GeocodingService] Rate limited by Nominatim for: " + placeName);
            } else {
                System.err.println("[GeocodingService] HTTP " + status + " for: " + placeName);
            }

        } catch (Exception e) {
            System.err.println("[GeocodingService] Error geocoding '" + placeName + "': " + e.getMessage());
        }

        return null;
    }

    /**
     * Returns the current cache size (useful for debug/logging).
     */
    public int getCacheSize() {
        return cache.size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parse lat/lon from Nominatim JSON response.
     *
     * Response format:
     * [
     *   {
     *     "lat": "36.8064948",
     *     "lon": "10.1815316",
     *     "display_name": "Tunis, Tunisia"
     *   }
     * ]
     *
     * Returns null if the array is empty or parsing fails.
     */
    private double[] parseCoords(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return null;

            JSONObject first = arr.getJSONObject(0);
            double lat = Double.parseDouble(first.getString("lat"));
            double lon = Double.parseDouble(first.getString("lon"));
            return new double[]{lat, lon};

        } catch (Exception e) {
            System.err.println("[GeocodingService] Parse error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Enforce Nominatim's 1 req/sec policy.
     * Synchronized on the class to coordinate all GeocodingService instances.
     */
    private static synchronized void enforceRateLimit() {
        long now   = System.currentTimeMillis();
        long delta = now - lastRequestTime;
        if (delta < MIN_INTERVAL_MS) {
            try { Thread.sleep(MIN_INTERVAL_MS - delta); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}