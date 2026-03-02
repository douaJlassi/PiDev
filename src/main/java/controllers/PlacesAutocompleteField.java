package controllers;

import entities.PlaceAutocomplete;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import services.PlacesAPIService;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * PlacesAutocompleteField - Custom JavaFX control for location autocomplete
 *
 * FEATURES:
 * - Extends TextField with autocomplete dropdown
 * - Debouncing: waits 300ms after user stops typing before API call
 * - Async API calls (non-blocking UI)
 * - Keyboard navigation (↑↓ arrows, Enter to select)
 * - Stores selected place_id internally
 * - Styled dropdown matching app theme
 * - Loading indicator while fetching
 * - Error handling with fallback to plain text
 *
 * USAGE:
 * PlacesAutocompleteField field = new PlacesAutocompleteField();
 * field.setPromptText("Enter location...");
 *
 * // Get selected place
 * String placeId = field.getSelectedPlaceId();      // e.g., "282402156"
 * String displayName = field.getSelectedPlaceName(); // e.g., "Tunis, Tunisia"
 *
 * // Or just get the text user typed (if they didn't select from dropdown)
 * String text = field.getText();
 *
 * DEPENDENCIES:
 * - PlacesAPIService (Nominatim)
 * - PlaceAutocomplete entity
 */
public class PlacesAutocompleteField extends TextField {

    private final PlacesAPIService apiService;
    private final ContextMenu suggestionsPopup;
    private Timer debounceTimer;

    // Selected place data
    private String selectedPlaceId = null;
    private String selectedPlaceName = null;

    // Configuration
    private static final int DEBOUNCE_DELAY_MS = 300; // Wait 300ms after typing stops
    private static final int MIN_CHARS = 2;           // Minimum characters before search
    private static final int MAX_SUGGESTIONS = 5;     // Maximum dropdown items

    // Loading indicator
    private boolean isLoading = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public PlacesAutocompleteField() {
        super();
        this.apiService = new PlacesAPIService();
        this.suggestionsPopup = new ContextMenu();

        setupAutocomplete();
        applyCustomStyling();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupAutocomplete() {
        // Listen for text changes
        textProperty().addListener((observable, oldValue, newValue) -> {
            onTextChanged(newValue);
        });

        // Hide popup when field loses focus
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !suggestionsPopup.isShowing()) {
                hideSuggestions();
            }
        });

        // Configure popup
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setHideOnEscape(true);
    }

    private void applyCustomStyling() {
        // Add style class for CSS targeting
        getStyleClass().add("places-autocomplete-field");
    }

    // ── Text Change Handler ───────────────────────────────────────────────────

    private void onTextChanged(String newText) {
        // Cancel any pending debounce timer
        if (debounceTimer != null) {
            debounceTimer.cancel();
        }

        // Clear selected place when user types (they're changing their selection)
        selectedPlaceId = null;
        selectedPlaceName = null;

        // Validate input length
        if (newText == null || newText.trim().length() < MIN_CHARS) {
            hideSuggestions();
            return;
        }

        // Debounce: wait 300ms before calling API
        debounceTimer = new Timer(true);
        debounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> fetchSuggestions(newText.trim()));
            }
        }, DEBOUNCE_DELAY_MS);
    }

    // ── API Call ──────────────────────────────────────────────────────────────

    private void fetchSuggestions(String query) {
        isLoading = true;
        showLoadingIndicator();

        apiService.getAutocompletePredictions(query)
                .thenAccept(predictions -> {
                    Platform.runLater(() -> {
                        isLoading = false;
                        displaySuggestions(predictions);
                    });
                })
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        isLoading = false;
                        System.err.println("Autocomplete error: " + error.getMessage());
                        hideSuggestions();
                    });
                    return null;
                });
    }

    // ── Display Suggestions ───────────────────────────────────────────────────

    private void displaySuggestions(List<PlaceAutocomplete> predictions) {
        suggestionsPopup.getItems().clear();

        if (predictions == null || predictions.isEmpty()) {
            showNoResultsMessage();
            return;
        }

        // Limit to MAX_SUGGESTIONS
        int count = Math.min(predictions.size(), MAX_SUGGESTIONS);

        for (int i = 0; i < count; i++) {
            PlaceAutocomplete place = predictions.get(i);
            CustomMenuItem item = createSuggestionItem(place);
            suggestionsPopup.getItems().add(item);
        }

        // Show popup below the text field
        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(this, Side.BOTTOM, 0, 0);
        }
    }

    private CustomMenuItem createSuggestionItem(PlaceAutocomplete place) {
        // Create visual structure for each suggestion
        VBox container = new VBox(2);
        container.getStyleClass().add("suggestion-item");

        // Main text (e.g., "Tunis")
        Label mainLabel = new Label(place.getMainText() != null
                ? place.getMainText()
                : place.getDescription());
        mainLabel.getStyleClass().add("suggestion-main-text");
        mainLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #050505;");

        container.getChildren().add(mainLabel);

        // Secondary text (e.g., "Tunisia")
        if (place.getSecondaryText() != null && !place.getSecondaryText().isEmpty()) {
            Label secondaryLabel = new Label(place.getSecondaryText());
            secondaryLabel.getStyleClass().add("suggestion-secondary-text");
            secondaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
            container.getChildren().add(secondaryLabel);
        }

        // Padding and hover effect
        container.setStyle(
                "-fx-padding: 10 14; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-color: white;"
        );

        container.setOnMouseEntered(e ->
                container.setStyle(
                        "-fx-padding: 10 14; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-color: #f0f2f5;"
                )
        );

        container.setOnMouseExited(e ->
                container.setStyle(
                        "-fx-padding: 10 14; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-color: white;"
                )
        );

        // Create menu item
        CustomMenuItem menuItem = new CustomMenuItem(container, false);

        // Handle selection
        menuItem.setOnAction(event -> {
            selectPlace(place);
            hideSuggestions();
        });

        return menuItem;
    }

    // ── Selection Handler ─────────────────────────────────────────────────────

    private void selectPlace(PlaceAutocomplete place) {
        // Store selected place data
        this.selectedPlaceId = place.getPlaceId();
        this.selectedPlaceName = place.getDescription();

        // Update text field with selected place name
        setText(place.getDisplayText());

        // Move cursor to end
        positionCaret(getText().length());
    }

    // ── Loading & Error States ────────────────────────────────────────────────

    private void showLoadingIndicator() {
        suggestionsPopup.getItems().clear();

        Label loadingLabel = new Label("Searching...");
        loadingLabel.setStyle(
                "-fx-text-fill: #65676b; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 10 14; " +
                        "-fx-font-style: italic;"
        );

        CustomMenuItem loadingItem = new CustomMenuItem(loadingLabel, false);
        suggestionsPopup.getItems().add(loadingItem);

        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(this, Side.BOTTOM, 0, 0);
        }
    }

    private void showNoResultsMessage() {
        suggestionsPopup.getItems().clear();

        Label noResultsLabel = new Label("No locations found");
        noResultsLabel.setStyle(
                "-fx-text-fill: #8a8d91; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 10 14; " +
                        "-fx-font-style: italic;"
        );

        CustomMenuItem noResultsItem = new CustomMenuItem(noResultsLabel, false);
        suggestionsPopup.getItems().add(noResultsItem);

        // Auto-hide after 2 seconds
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> hideSuggestions());
            }
        }, 2000);

        if (!suggestionsPopup.isShowing()) {
            suggestionsPopup.show(this, Side.BOTTOM, 0, 0);
        }
    }

    private void hideSuggestions() {
        if (suggestionsPopup.isShowing()) {
            suggestionsPopup.hide();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Get the place_id of the selected location (or null if none selected)
     */
    public String getSelectedPlaceId() {
        return selectedPlaceId;
    }

    /**
     * Get the full display name of the selected location (or null if none selected)
     */
    public String getSelectedPlaceName() {
        return selectedPlaceName;
    }

    /**
     * Check if user has selected a place from the dropdown (vs just typing)
     */
    public boolean hasSelectedPlace() {
        return selectedPlaceId != null;
    }

    /**
     * Clear the selected place and text
     */
    public void clearSelection() {
        selectedPlaceId = null;
        selectedPlaceName = null;
        setText("");
        hideSuggestions();
    }

    /**
     * Programmatically set a place (useful for editing existing posts)
     * @param placeId The place ID
     * @param displayName The display name to show in the field
     */
    public void setSelectedPlace(String placeId, String displayName) {
        this.selectedPlaceId = placeId;
        this.selectedPlaceName = displayName;
        setText(displayName);
    }

    /**
     * Get text or selected place name (fallback to typed text)
     */
    public String getLocationText() {
        return selectedPlaceName != null ? selectedPlaceName : getText();
    }
}