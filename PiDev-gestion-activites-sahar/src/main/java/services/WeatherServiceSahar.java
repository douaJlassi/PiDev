package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import utils.Config;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WeatherServiceSahar {
    private static final String API_KEY = Config.get("weather.api.key");
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/forecast";
    private final HttpClient httpClient;
    private final Map<String, WeatherInfo> cache = new ConcurrentHashMap<>();

    public WeatherServiceSahar() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public CompletableFuture<WeatherInfo> getWeatherForCityAndDateTime(String city, LocalDateTime dateTime) {
        String key = city + "_" + dateTime.toString();
        if (cache.containsKey(key)) {
            return CompletableFuture.completedFuture(cache.get(key));
        }

        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = BASE_URL + "?q=" + encodedCity + "&appid=" + API_KEY + "&units=metric&lang=fr";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(this::parseForecast)
                    .thenApply(forecasts -> findClosestForecast(forecasts, dateTime))
                    .thenApply(weather -> {
                        if (weather != null) cache.put(key, weather);
                        return weather;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private List<WeatherInfo> parseForecast(String responseBody) {
        List<WeatherInfo> list = new ArrayList<>();
        JsonObject obj = JsonParser.parseString(responseBody).getAsJsonObject();
        if (!obj.has("list")) return list;
        JsonArray arr = obj.getAsJsonArray("list");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject item = arr.get(i).getAsJsonObject();
            long dt = item.get("dt").getAsLong();
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(dt), ZoneId.systemDefault());
            JsonObject main = item.getAsJsonObject("main");
            double temp = main.get("temp").getAsDouble();
            JsonArray weatherArr = item.getAsJsonArray("weather");
            String description = weatherArr.get(0).getAsJsonObject().get("description").getAsString();
            String icon = weatherArr.get(0).getAsJsonObject().get("icon").getAsString();
            list.add(new WeatherInfo(dateTime, temp, description, icon));
        }
        return list;
    }

    private WeatherInfo findClosestForecast(List<WeatherInfo> forecasts, LocalDateTime target) {
        return forecasts.stream()
                .min(Comparator.comparingLong(f -> Math.abs(f.getDateTime().until(target, java.time.temporal.ChronoUnit.SECONDS))))
                .orElse(null);
    }

    public static class WeatherInfo {
        private final LocalDateTime dateTime;
        private final double temperature;
        private final String description;
        private final String iconCode;

        public WeatherInfo(LocalDateTime dateTime, double temperature, String description, String iconCode) {
            this.dateTime = dateTime;
            this.temperature = temperature;
            this.description = description;
            this.iconCode = iconCode;
        }

        public LocalDateTime getDateTime() { return dateTime; }
        public double getTemperature() { return temperature; }
        public String getDescription() { return description; }
        public String getIconCode() { return iconCode; }
        public String getIconUrl() { return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png"; }
    }
}