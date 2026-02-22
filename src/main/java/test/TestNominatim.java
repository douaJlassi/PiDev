package test;
import services.PlacesAPIService;

public class TestNominatim {
    public static void main(String[] args) {
        PlacesAPIService service = new PlacesAPIService();

        // Test connectivity
        boolean connected = service.testConnection();

        if (connected) {
            System.out.println("✓ Nominatim API is ready!");

            // Test with Tunisia location
            service.getAutocompletePredictions("Tunis")
                    .thenAccept(predictions -> {
                        System.out.println("\nFound " + predictions.size() + " results:");
                        predictions.forEach(p ->
                                System.out.println("  - " + p.getDisplayText())
                        );
                    });

            // Keep main thread alive to see async results
            try { Thread.sleep(3000); } catch (InterruptedException e) {}

        } else {
            System.err.println("✗ API connection failed. Check your internet connection.");
        }
    }
}
