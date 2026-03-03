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
        String b = base.toUpperCase();
        String t = target.toUpperCase();
        if (b.equals(t)) return BigDecimal.ONE;

        String key = b + "->" + t;
        CachedRate c = cache.get(key);
        if (c != null && Duration.between(c.at, Instant.now()).compareTo(ttl) < 0) return c.rate;

        // ✅ Provider 1: Frankfurter (no key)
        // Example: https://api.frankfurter.dev/v1/latest?from=EUR&to=USD
        String url1 = "https://api.frankfurter.dev/v1/latest?from=" + b + "&to=" + t;

        // ✅ Provider 2: open.er-api (no key)
        // Example: https://open.er-api.com/v6/latest/EUR  -> rates.USD
        String url2 = "https://open.er-api.com/v6/latest/" + b;

        try {
            String body = fetch(url1);
            BigDecimal rate = parseFrankfurter(body, t);
            cache.put(key, new CachedRate(rate, Instant.now()));
            return rate;
        } catch (Exception first) {
            try {
                String body = fetch(url2);
                BigDecimal rate = parseOpenErApi(body, t);
                cache.put(key, new CachedRate(rate, Instant.now()));
                return rate;
            } catch (Exception second) {
                throw new RuntimeException("Exchange rate failed (" + b + "->" + t + "): " + second.getMessage(), second);
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

    // Frankfurter JSON: { "base":"EUR", "date":"...", "rates": { "USD": 1.08 } }
    private BigDecimal parseFrankfurter(String body, String targetUpper) {
        return parseRateInsideObject(body, "\"rates\"", "\"" + targetUpper + "\"");
    }

    // open.er-api JSON: { "base_code":"EUR", "rates": { "USD": 1.08, ... } }
    private BigDecimal parseOpenErApi(String body, String targetUpper) {
        return parseRateInsideObject(body, "\"rates\"", "\"" + targetUpper + "\"");
    }

    // Finds: <objectName> : { ... <targetKey> : <number> ... }
    private BigDecimal parseRateInsideObject(String body, String objectName, String targetKey) {
        int objIdx = body.indexOf(objectName);
        if (objIdx < 0) throw new RuntimeException("rates object not found");

        int braceStart = body.indexOf('{', objIdx);
        if (braceStart < 0) throw new RuntimeException("rates brace not found");

        int targetIdx = body.indexOf(targetKey, braceStart);
        if (targetIdx < 0) throw new RuntimeException("Target currency not found: " + targetKey);

        int colon = body.indexOf(':', targetIdx);
        if (colon < 0) throw new RuntimeException("Invalid JSON near target");

        int start = colon + 1;
        while (start < body.length() && (body.charAt(start) == ' ')) start++;

        int end = start;
        while (end < body.length() && "0123456789.-".indexOf(body.charAt(end)) >= 0) end++;

        if (end <= start) throw new RuntimeException("Invalid number for " + targetKey);

        return new BigDecimal(body.substring(start, end));
    }

    public BigDecimal convert(BigDecimal amount, String base, String target) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal rate = getRate(base, target);
        return amount.multiply(rate);
    }
}