package entities;

/**
 * PlaceAutocomplete - Represents a single autocomplete suggestion from Google Places API
 *
 * Maps to the "predictions" array in the API response:
 * {
 *   "description": "Tunis, Tunisia",
 *   "place_id": "ChIJ01QZRgQ-fxIRH8vKqjqzKNk",
 *   "structured_formatting": {
 *     "main_text": "Tunis",
 *     "secondary_text": "Tunisia"
 *   }
 * }
 */
public class PlaceAutocomplete {

    private String placeId;        // Unique Google Place ID
    private String description;    // Full description (e.g., "Tunis, Tunisia")
    private String mainText;       // Primary text (e.g., "Tunis")
    private String secondaryText;  // Secondary text (e.g., "Tunisia")

    // ── Constructors ──────────────────────────────────────────────────────────

    public PlaceAutocomplete() {}

    public PlaceAutocomplete(String placeId, String description) {
        this.placeId = placeId;
        this.description = description;
    }

    public PlaceAutocomplete(String placeId, String description,
                             String mainText, String secondaryText) {
        this.placeId = placeId;
        this.description = description;
        this.mainText = mainText;
        this.secondaryText = secondaryText;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPlaceId() { return placeId; }
    public void setPlaceId(String placeId) { this.placeId = placeId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMainText() { return mainText; }
    public void setMainText(String mainText) { this.mainText = mainText; }

    public String getSecondaryText() { return secondaryText; }
    public void setSecondaryText(String secondaryText) { this.secondaryText = secondaryText; }

    // ── Utility methods ───────────────────────────────────────────────────────

    /**
     * Get display text for UI (uses structured formatting if available)
     */
    public String getDisplayText() {
        if (mainText != null && secondaryText != null) {
            return mainText + ", " + secondaryText;
        }
        return description != null ? description : "";
    }

    /**
     * Check if this is a valid prediction (has required fields)
     */
    public boolean isValid() {
        return placeId != null && !placeId.trim().isEmpty()
                && description != null && !description.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "PlaceAutocomplete{placeId='" + placeId + "', description='" + description + "'}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PlaceAutocomplete)) return false;
        PlaceAutocomplete other = (PlaceAutocomplete) obj;
        return placeId != null && placeId.equals(other.placeId);
    }

    @Override
    public int hashCode() {
        return placeId != null ? placeId.hashCode() : 0;
    }
}