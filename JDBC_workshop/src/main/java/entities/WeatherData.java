package entities;

import java.io.Serializable;

/**
 * WeatherData - Represents current weather information for a location.
 *
 * Maps to OpenWeatherMap API response:
 * <pre>
 * {
 *   "weather": [{"main": "Clear", "description": "clear sky", "icon": "01d"}],
 *   "main": {"temp": 28.5, "feels_like": 29.2, "humidity": 45},
 *   "name": "Tunis"
 * }
 * </pre>
 */
public class WeatherData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String locationName;        // City name (e.g., "Tunis")
    private double temperature;         // Temperature in Celsius
    private double feelsLike;           // "Feels like" temperature
    private int    humidity;            // Humidity percentage (0–100)
    private String weatherMain;         // Main condition  (e.g., "Clear", "Clouds")
    private String weatherDescription;  // Detailed description (e.g., "clear sky")
    private String iconCode;            // Weather icon code (e.g., "01d")
    private long   timestamp;           // When this data was fetched (cache expiration)

    // ── Constructors ──────────────────────────────────────────────────────────

    public WeatherData() {
        this.timestamp = System.currentTimeMillis();
    }

    public WeatherData(String locationName, double temperature, String weatherMain,
                       String weatherDescription, String iconCode) {
        this.locationName        = locationName;
        this.temperature         = temperature;
        this.weatherMain         = weatherMain;
        this.weatherDescription  = weatherDescription;
        this.iconCode            = iconCode;
        this.timestamp           = System.currentTimeMillis();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getLocationName()                        { return locationName; }
    public void   setLocationName(String locationName)     { this.locationName = locationName; }

    public double getTemperature()                         { return temperature; }
    public void   setTemperature(double temperature)       { this.temperature = temperature; }

    public double getFeelsLike()                           { return feelsLike; }
    public void   setFeelsLike(double feelsLike)           { this.feelsLike = feelsLike; }

    public int    getHumidity()                            { return humidity; }
    public void   setHumidity(int humidity)                { this.humidity = humidity; }

    public String getWeatherMain()                         { return weatherMain; }
    public void   setWeatherMain(String weatherMain)       { this.weatherMain = weatherMain; }

    public String getWeatherDescription()                              { return weatherDescription; }
    public void   setWeatherDescription(String weatherDescription)     { this.weatherDescription = weatherDescription; }

    public String getIconCode()                            { return iconCode; }
    public void   setIconCode(String iconCode)             { this.iconCode = iconCode; }

    public long   getTimestamp()                           { return timestamp; }
    public void   setTimestamp(long timestamp)             { this.timestamp = timestamp; }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Get temperature as a formatted string with unit.
     * @param useCelsius true for Celsius, false for Fahrenheit
     */
    public String getFormattedTemperature(boolean useCelsius) {
        if (useCelsius) {
            return String.format("%.0f°C", temperature);
        } else {
            double fahrenheit = (temperature * 9.0 / 5) + 32;
            return String.format("%.0f°F", fahrenheit);
        }
    }

    /** Get temperature as a formatted string (Celsius by default). */
    public String getFormattedTemperature() {
        return getFormattedTemperature(true);
    }

    /** Get URL for the weather icon. */
    public String getIconUrl() {
        if (iconCode == null || iconCode.isEmpty()) return null;
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }

    /** Get emoji representation of the weather condition. */
    public String getWeatherEmoji() {
        if (weatherMain == null) return "🌡️";
        switch (weatherMain.toLowerCase()) {
            case "clear":                          return (iconCode != null && iconCode.endsWith("n")) ? "🌙" : "☀️";
            case "clouds":                         return "☁️";
            case "rain": case "drizzle":           return "🌧️";
            case "thunderstorm":                   return "⛈️";
            case "snow":                           return "❄️";
            case "mist": case "fog": case "haze":  return "🌫️";
            default:                               return "🌡️";
        }
    }

    /** Returns true if the data is less than 30 minutes old. */
    public boolean isFresh() {
        long ageMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60);
        return ageMinutes < 30;
    }

    /** Age of this data in minutes. */
    public long getAgeMinutes() {
        return (System.currentTimeMillis() - timestamp) / (1000 * 60);
    }

    /** Returns true if all required fields are populated. */
    public boolean isValid() {
        return locationName != null && !locationName.isEmpty()
                && weatherMain  != null && !weatherMain.isEmpty()
                && iconCode     != null && !iconCode.isEmpty();
    }

    /** Tooltip text with detailed weather info. */
    public String getTooltipText() {
        StringBuilder sb = new StringBuilder();
        sb.append(weatherDescription != null ? capitalize(weatherDescription) : weatherMain);
        sb.append("\nTemperature: ").append(getFormattedTemperature());
        if (feelsLike != 0 && Math.abs(feelsLike - temperature) > 2) {
            sb.append(" (feels like ").append(String.format("%.0f°C", feelsLike)).append(")");
        }
        if (humidity > 0) {
            sb.append("\nHumidity: ").append(humidity).append("%");
        }
        return sb.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "location='" + locationName + '\'' +
                ", temp=" + temperature +
                ", weather='" + weatherMain + '\'' +
                ", icon='" + iconCode + '\'' +
                ", age=" + getAgeMinutes() + "min" +
                '}';
    }
}