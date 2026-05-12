package utils;

import entities.WeatherData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WeatherCache - In-memory LRU cache for weather data
 *
 * PURPOSE:
 * - Reduce API calls to OpenWeatherMap
 * - Improve performance (instant weather display from cache)
 * - Stay within rate limits (1,000 calls/day)
 *
 * STRATEGY:
 * - Cache by location name (e.g., "Tunis, Tunisia")
 * - Cache TTL: 30 minutes (weather doesn't change that fast)
 * - Max cache size: 500 entries (LRU eviction)
 * - Thread-safe (ConcurrentHashMap)
 *
 * USAGE:
 * WeatherCache cache = WeatherCache.getInstance();
 *
 * // Try cache first
 * WeatherData cached = cache.get("Tunis, Tunisia");
 * if (cached != null) {
 *     // Use cached data
 * } else {
 *     // Fetch from API
 *     WeatherData fresh = weatherService.fetchWeather("Tunis, Tunisia");
 *     cache.put("Tunis, Tunisia", fresh);
 * }
 */
public class WeatherCache {

    private static WeatherCache instance;

    private final Map<String, WeatherData> cache;
    private static final int MAX_CACHE_SIZE = 500;
    private static final long CACHE_TTL_MINUTES = 30;

    private WeatherCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Get singleton instance
     */
    public static synchronized WeatherCache getInstance() {
        if (instance == null) {
            instance = new WeatherCache();
        }
        return instance;
    }

    /**
     * Get weather data from cache (null if not found or expired)
     *
     * @param locationKey Location name (e.g., "Tunis, Tunisia")
     * @return Cached weather data or null
     */
    public WeatherData get(String locationKey) {
        if (locationKey == null || locationKey.trim().isEmpty()) {
            return null;
        }

        String key = normalizeKey(locationKey);
        WeatherData data = cache.get(key);

        // Check if data exists and is still fresh
        if (data != null && data.isFresh()) {
            return data;
        }

        // Data expired or doesn't exist
        if (data != null) {
            cache.remove(key); // Remove stale data
        }

        return null;
    }

    /**
     * Store weather data in cache
     *
     * @param locationKey Location name
     * @param data Weather data to cache
     */
    public void put(String locationKey, WeatherData data) {
        if (locationKey == null || data == null || !data.isValid()) {
            return;
        }

        String key = normalizeKey(locationKey);

        // Enforce max cache size (simple LRU: remove oldest entry)
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        cache.put(key, data);
    }

    /**
     * Remove weather data from cache
     */
    public void remove(String locationKey) {
        if (locationKey != null) {
            cache.remove(normalizeKey(locationKey));
        }
    }

    /**
     * Clear entire cache
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get current cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if location is in cache and fresh
     */
    public boolean contains(String locationKey) {
        return get(locationKey) != null;
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        int totalEntries = cache.size();
        int freshEntries = 0;
        int staleEntries = 0;

        for (WeatherData data : cache.values()) {
            if (data.isFresh()) {
                freshEntries++;
            } else {
                staleEntries++;
            }
        }

        return new CacheStats(totalEntries, freshEntries, staleEntries);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Normalize location key (lowercase, trim)
     */
    private String normalizeKey(String locationKey) {
        return locationKey.trim().toLowerCase();
    }

    /**
     * Evict oldest entry (simple LRU implementation)
     */
    private void evictOldest() {
        WeatherData oldest = null;
        String oldestKey = null;

        for (Map.Entry<String, WeatherData> entry : cache.entrySet()) {
            if (oldest == null || entry.getValue().getTimestamp() < oldest.getTimestamp()) {
                oldest = entry.getValue();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }

    /**
     * Clean up expired entries (call periodically to free memory)
     */
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> !entry.getValue().isFresh());
    }

    // ── Cache Statistics Inner Class ──────────────────────────────────────────

    public static class CacheStats {
        public final int totalEntries;
        public final int freshEntries;
        public final int staleEntries;

        public CacheStats(int totalEntries, int freshEntries, int staleEntries) {
            this.totalEntries = totalEntries;
            this.freshEntries = freshEntries;
            this.staleEntries = staleEntries;
        }

        public double getHitRate() {
            return totalEntries > 0 ? (double) freshEntries / totalEntries : 0.0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, fresh=%d, stale=%d, hitRate=%.1f%%}",
                    totalEntries, freshEntries, staleEntries, getHitRate() * 100);
        }
    }
}