package services;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExchangeRateService {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static class CachedRate {
        BigDecimal rate;
        Instant at;
        CachedRate(BigDecimal rate, Instant at){ this.rate=rate; this.at=at; }
    }

    private final Map<String, CachedRate> cache = new ConcurrentHashMap<>();
    private final Duration ttl = Duration.ofMinutes(30);

    public BigDecimal getRate(String base, String target) {
        String key = base.toUpperCase() + "->" + target.toUpperCase();
        CachedRate c = cache.get(key);
        if (c != null && Duration.between(c.at, Instant.now()).compareTo(ttl) < 0) return c.rate;

        String baseLc = base.toLowerCase();
        String targetLc = target.toLowerCase();

        String url1 = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/" + baseLc + ".json";
        String url2 = "https://latest.currency-api.pages.dev/v1/currencies/" + baseLc + ".json"; // fallback

        try {
            String body = fetch(url1);
            BigDecimal rate = parseRate(body, baseLc, targetLc);

            cache.put(key, new CachedRate(rate, Instant.now()));
            return rate;

        } catch (Exception first) {
            try {
                String body = fetch(url2);
                BigDecimal rate = parseRate(body, baseLc, targetLc);

                cache.put(key, new CachedRate(rate, Instant.now()));
                return rate;

            } catch (Exception second) {
                throw new RuntimeException("Exchange rate failed (" + base + "->" + target + "): " + second.getMessage(), second);
            }
        }
    }

    private String fetch(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("HTTP " + res.statusCode());
        return res.body();
    }

    // JSON shape: { "date":"...", "tnd": { "eur": 0.29, "usd": 0.31, ... } }
    private BigDecimal parseRate(String body, String baseLc, String targetLc) {
        String baseMarker = "\"" + baseLc + "\":";
        int baseIdx = body.indexOf(baseMarker);
        if (baseIdx < 0) throw new RuntimeException("Base currency not found: " + baseLc);

        String targetMarker = "\"" + targetLc + "\":";
        int idx = body.indexOf(targetMarker, baseIdx);
        if (idx < 0) throw new RuntimeException("Target currency not found: " + targetLc);

        int start = idx + targetMarker.length();

        // skip spaces/colon
        while (start < body.length() && (body.charAt(start) == ' ' || body.charAt(start) == ':')) start++;

        int end = start;
        while (end < body.length() && "0123456789.-".indexOf(body.charAt(end)) >= 0) end++;

        if (end <= start) throw new RuntimeException("Invalid number for " + targetLc);

        return new BigDecimal(body.substring(start, end));
    }

    public BigDecimal convert(BigDecimal amount, String base, String target) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal rate = getRate(base, target);
        return amount.multiply(rate);
    }
}