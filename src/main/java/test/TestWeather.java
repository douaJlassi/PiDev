package test;
import services.WeatherService;
import utils.WeatherConfig;

public class TestWeather {
    public static void main(String[] args) {
        // Check configuration
        System.out.println("API Key configured: " + WeatherConfig.isConfigured());
        System.out.println("API Key valid format: " +
                WeatherConfig.isValidKeyFormat(WeatherConfig.getApiKey()));

        // Test connectivity
        WeatherService service = new WeatherService();
        boolean connected = service.testConnection();

        if (connected) {
            System.out.println("✓ OpenWeatherMap API is ready!");

            // Test with Tunisia
            service.getWeatherByLocation("Tunis,TN")
                    .thenAccept(weather -> {
                        if (weather != null) {
                            System.out.println("\nWeather in Tunis:");
                            System.out.println("  Temperature: " + weather.getFormattedTemperature());
                            System.out.println("  Condition: " + weather.getWeatherMain());
                            System.out.println("  Description: " + weather.getWeatherDescription());
                            System.out.println("  Emoji: " + weather.getWeatherEmoji());
                        }
                    });

            // Keep main thread alive to see async results
            try { Thread.sleep(5000); } catch (InterruptedException e) {}

        } else {
            System.err.println("✗ API connection failed. Check:");
            System.err.println("  1. API key is correct (32 characters)");
            System.err.println("  2. Key has been activated (wait 2 hours after signup)");
            System.err.println("  3. Internet connection is working");
        }
    }
}